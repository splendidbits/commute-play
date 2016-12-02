package actors;

import enums.AgencyUpdateType;

import javax.annotation.Nonnull;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 3/28/16 Splendid Bits.
 */
public class AgencyUpdateMessage implements AgencyUpdateProtocol {
    private AgencyUpdateType mAgencyUpdateType;

    public AgencyUpdateMessage(@Nonnull AgencyUpdateType agencyUpdateType) {
        mAgencyUpdateType = agencyUpdateType;
    }

    @Override
    public AgencyUpdateType getAgencyType() {
        return mAgencyUpdateType;
    }
}
