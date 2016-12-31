package main;

import actors.AgencyUpdateActor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import play.Application;
import play.Environment;
import play.inject.guice.GuiceApplicationBuilder;
import play.test.Helpers;

import static play.inject.Bindings.bind;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 28/12/2016 Splendid Bits.
 */
public abstract class AbstractApplicationTest {
    protected static Application application;

    @BeforeClass
    public static void startApplicationTest() {
        application = new GuiceApplicationBuilder()
                .in(Environment.simple())
                .overrides(bind(AgencyUpdateActor.class).to(MockComponent.class))
                .overrides(bind(CommuteSchedulers.class).to(MockComponent.class))
                .build();
    }

    @AfterClass
    public static void stopApplicationTest() {
        Helpers.stop(application);
    }
}
