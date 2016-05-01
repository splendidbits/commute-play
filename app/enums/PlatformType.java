package enums;

import com.avaje.ebean.annotation.EnumValue;

/**
 * The type of push messaging service (platform) that is being used.
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/30/16 Splendid Bits.
 */
public enum PlatformType {
    @EnumValue("GCM")
    SERVICE_GCM("GCM"),

    @EnumValue("APNS")
    SERVICE_APNS("APNS");

    public String name;

    PlatformType(String name) {
        this.name = name;
    }
}
