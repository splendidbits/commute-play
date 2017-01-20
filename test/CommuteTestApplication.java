import injection.modules.DatabaseModule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import play.Application;
import play.ApplicationLoader;
import play.Environment;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.Helpers;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 28/12/2016 Splendid Bits.
 */
public abstract class CommuteTestApplication {
    protected static Application application;

    @BeforeClass
    public static void startApplicationTest() {
        ApplicationLoader.Context context = new ApplicationLoader.Context(Environment.simple());
        application = new GuiceApplicationBuilder()
                .configure(context.initialConfiguration())
                .in(context.environment())
                .bindings(new DatabaseModule())
                .build();
    }

    @AfterClass
    public static void stopApplicationTest() {
        Helpers.stop(application);
    }

}
