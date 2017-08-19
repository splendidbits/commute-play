package services;

import dao.AgencyDao;
import models.alerts.Agency;
import models.alerts.Route;
import play.cache.CacheApi;
import play.cache.NamedCache;
import services.fluffylog.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Receives agency bundles and performs the following actions.
 * <p>
 * 1: Iterate through each Route > Alert bundle and find any differences from the previous set.
 * 3: Collect the new alerts.
 * 4: Persist new data.
 * 5: Get list of subscriptions for route
 * 6: send data in batches of 1000 to google.
 */
public class AgencyManager {
    private static final String CACHE_ALL_KEY = "cache_agency_all";
    private static final String CACHE_AGENCY_KEY = "cache_agency_%d";

    private CacheApi mCacheApi;
    private AgencyDao mAgencyDao;

    @Inject
    public AgencyManager(AgencyDao agencyDao, @NamedCache("agency_cache") CacheApi cacheApi) {
        mAgencyDao = agencyDao;
        mCacheApi = cacheApi;
    }

    /**
     * Persist an agency to the datastore and cache.
     *
     * @param agency agency to persist.
     * @return boolean of success.
     */
    public boolean saveAgency(Agency agency) {
        boolean agencySaved = false;
        if (agency != null) {
            agencySaved = mAgencyDao.saveAgency(agency);
            cacheAgency(agency);

        }
        return agencySaved;
    }

    /**
     * Retrieve an {@link Agency} from either the cache or backing datastore.
     *
     * @param agencyId agencyId for Agency to fetch.
     * @return A saved agency if it exists, or null if not.
     */
    @Nullable
    public Agency getSavedAgency(int agencyId) {
        Agency agency = getCachedAgency(agencyId);
        if (agency == null) {
            return mAgencyDao.getAgency(agencyId);
        }
        return agency;
    }

    /**
     * Retrieve an list of {@link Agency} stubs (without any sub objects) from
     * the agency cache.
     *
     * @return A list of {@link Agency} with all sub-models. Null if never cached or stale.
     */
    @Nonnull
    public List<Agency> getCachedAgencyMetadata() {
        List<Agency> agencies = mCacheApi.get(CACHE_ALL_KEY);
        return agencies != null ? agencies : new ArrayList<>();
    }

    /**
     * Cache an an agency for a period of time. Also cache thes stub - or metadata - (that is the
     * {@link Agency} *without* corresponding decedent (eg {@link Route}) objects, in a
     * "master" Agencies cache for other queries.
     *
     * @param agency agency to set as the cache.
     */
    private void cacheAgency(@Nullable Agency agency) {
        if (agency != null && agency.id != null) {
            String agencyCacheKey = String.format(Locale.US, CACHE_AGENCY_KEY, agency.id);

            mCacheApi.set(agencyCacheKey, agency);
            Logger.info(String.format("Saved agency %d to %s.", agency.id, agencyCacheKey));

            // Iterate through the list of cached agencies and check it contains this one.
            List<Agency> cachedAgencies = getCachedAgencyMetadata();
            if (!cachedAgencies.isEmpty()) {
                Iterator<Agency> cachedAgencyIterator = cachedAgencies.iterator();

                //for (Agency cachedAgency : cachedAgencies) {
                while (cachedAgencyIterator.hasNext()) {
                    Agency cachedAgency = cachedAgencyIterator.next();

                    // Find a matching Agency Identifier.
                    if (agency.id.equals(cachedAgency.id)) {
                        cachedAgencyIterator.remove();
                    }
                }
            }

            // There was no cache match found for the agency. Add it to the cache.
            cachedAgencies.add(agency);
            mCacheApi.set(CACHE_ALL_KEY, cachedAgencies);
            Logger.info(String.format("Saved agency %d to %s.", agency.id, CACHE_ALL_KEY));
        }
    }

    /**
     * Retrieve an agency from the agency cache.
     *
     * @param agencyId the Id of the {@link Agency} to retrieve.
     * @return An {@link Agency} with all sub-models. Null if never cached or stale.
     */
    @Nullable
    public Agency getCachedAgency(int agencyId) {
        String agencyCacheKey = String.format(Locale.US, CACHE_AGENCY_KEY, agencyId);
        Agency agency = mCacheApi.get(agencyCacheKey);

        if (agency != null) {
            Logger.info(String.format("Cache hit for agency %d in %s. :)", agencyId, agencyCacheKey));

        } else {
            Logger.warn(String.format("Cache miss for agency %d in %s. :(", agencyId, agencyCacheKey));
        }
        return agency;
    }
}
