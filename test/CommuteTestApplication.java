import injection.modules.DatabaseModule;
import main.fluffylog.FluffyLogModule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import play.Application;
import play.Mode;
import play.api.inject.Binding;
import play.api.inject.BindingKey;
import play.api.inject.guice.GuiceableModule;
import play.api.inject.guice.GuiceableModule$;
import play.api.routing.Router;
import play.inject.guice.GuiceApplicationBuilder;
import play.routing.RoutingDsl;
import play.test.Helpers;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 28/12/2016 Splendid Bits.
 */
public abstract class CommuteTestApplication {
    static Application application;

    @BeforeClass
    public static void startApplicationTest() {
        // Mock the router binding.
        Binding<Router> routesBindingOverride = new BindingKey<>(Router.class)
                .toProvider(MockRouterProvider.class)
                .eagerly();

        // Create a module from a single binding.
        GuiceableModule module = GuiceableModule$.MODULE$.fromPlayBinding(routesBindingOverride);

        application = new GuiceApplicationBuilder()
                .in(Mode.TEST)
//                .disable(PushServicesModule.class)
                .overrides(routesBindingOverride)
                .bindings(new DatabaseModule())
                .bindings(new FluffyLogModule())
                .build();
    }

    @AfterClass
    public static void stopApplicationTest() {
        Helpers.stop(application);
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