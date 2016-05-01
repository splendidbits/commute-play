package enums;

import com.avaje.ebean.annotation.EnumValue;

/**
 * The type of route. (route features).
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/30/16 Splendid Bits.
 */
public enum RouteFlag {

    @EnumValue("OWL")
    TYPE_OWL("OWL"),

    @EnumValue("PRIVATE")
    TYPE_PRIVATE("PRIVATE"),

    @EnumValue("TEMPORARY_ROUTE")
    TYPE_TEMPORARY_ROUTE("TEMPORARY_ROUTE"),

    @EnumValue("CLOSED_TEMPORARILY")
    TYPE_CLOSED_TEMPORARILY("CLOSED_TEMPORARILY"),

    @EnumValue("CLOSED_PERMANENTLY")
    TYPE_CLOSED_PERMANENTLY("CLOSED_PERMANENTLY");

    public String featureName;

    RouteFlag(String featureName) {
        this.featureName = featureName;
    }
}
