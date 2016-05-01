package enums;

import com.avaje.ebean.annotation.EnumValue;

/**
 * The transport type for a route. (trolley, bus, etc).
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/30/16 Splendid Bits.
 */
public enum TransitType {

    @EnumValue("SPECIAL")
    TYPE_SPECIAL("SPECIAL"),

    @EnumValue("BUS")
    TYPE_BUS("BUS"),

    @EnumValue("RAIL")
    TYPE_RAIL("RAIL"),

    @EnumValue("LIGHT_RAIL")
    TYPE_LIGHT_RAIL("LIGHT_RAIL"),

    @EnumValue("SUBWAY")
    TYPE_SUBWAY("SUBWAY"),

    @EnumValue("CABLE")
    TYPE_CABLE("CABLE"),

    @EnumValue("FERRY")
    TYPE_FERRY("FERRY"),

    @EnumValue("BIKE_SHARE")
    TYPE_BIKE_SHARE("BIKE_SHARE");

    public String transportType;

    TransitType(String transportType) {
        this.transportType = transportType;
    }
}
