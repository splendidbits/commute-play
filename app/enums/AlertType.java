package enums;

import io.ebean.annotation.EnumValue;

/**
 * Defines different types of Agency {@link models.alerts.Alert} messages.
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/30/16 Splendid Bits.
 */
public enum AlertType {
    @EnumValue("")
    TYPE_NONE(""),

    @EnumValue("MAINTENANCE")
    TYPE_MAINTENANCE("Route Maintenance"),

    @EnumValue("DISRUPTION")
    TYPE_DISRUPTION("Route Disruption"),

    @EnumValue("INFORMATION")
    TYPE_INFORMATION("Route Information"),

    @EnumValue("DETOUR")
    TYPE_DETOUR("Route Detour"),

    @EnumValue("WEATHER")
    TYPE_WEATHER("Weather Alert"),

    @EnumValue("IN_APP")
    TYPE_IN_APP("Application Message");

    public String title;

    AlertType(String title) {
        this.title = title;
    }
}