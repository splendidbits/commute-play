import dao.AccountDao;
import dao.AgencyDao;
import dao.DeviceDao;
import injection.modules.ApplicationModule;
import injection.modules.DatabaseModule;
import injection.pushservices.modules.PushServicesModule;
import main.LifecycleListener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import play.Application;
import play.Mode;
import play.api.inject.Binding;
import play.api.inject.BindingKey;
import play.api.routing.Router;
import play.inject.guice.GuiceApplicationBuilder;
import play.routing.RoutingDsl;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 28/12/2016 Splendid Bits.
 */
public abstract class CommuteTestApplication {
    static Application application = null;
    static AccountDao mAccountDao = null;
    static DeviceDao mDeviceDao = null;
    static AgencyDao mAgencyDao = null;


    @BeforeClass
    public static void startApplicationTest() {
        // Mock the router binding.
        if (application == null) {
            Binding<Router> routesBindingOverride = new BindingKey<>(Router.class)
                    .toProvider(MockRouterProvider.class)
                    .eagerly();

            application = new GuiceApplicationBuilder()
                    .in(Mode.TEST)
                    .configure("db.commutealerts.name", "commutealerts")
                    .configure("db.commutealerts.driver", "org.postgresql.Driver")
                    .configure("db.commutealerts.url", "jdbc:postgresql://localhost:5432/commutealerts")
                    .configure("db.commutealerts.username", "splendidbits")
                    .configure("db.commutealerts.password", "kYBaf34sfd8L")
                    .configure("db.commutealerts.databasePlatformName", "postgres")
                    .overrides(routesBindingOverride)
                    .bindings(new ApplicationModule())
                    .bindings(new DatabaseModule())
                    .bindings(new PushServicesModule())
                    .disable(LifecycleListener.class)
                    .build();

            mAccountDao = application.injector().instanceOf(AccountDao.class);
            mDeviceDao = application.injector().instanceOf(DeviceDao.class);
            mAgencyDao = application.injector().instanceOf(AgencyDao.class);
        }
    }

    @AfterClass
    public static void stopApplicationTest() {

    }

    /**
     * Override the default routes.
     */
    static class MockRouterProvider implements Provider<Router> {
        private final RoutingDsl routingDsl;

        @Inject
        public MockRouterProvider(RoutingDsl routingDsl) {
            this.routingDsl = routingDsl;
        }

        @Override
        public play.api.routing.Router get() {
            return routingDsl.build().asScala();
        }
    }
}