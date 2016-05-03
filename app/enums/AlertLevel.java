package enums;

import com.avaje.ebean.annotation.EnumValue;

/**
 * Defines different levels for Agency {@link models.alerts.Alert} messages.
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/30/16 Splendid Bits.
 */
public enum AlertLevel {

    @EnumValue("SILENT")
    LEVEL_SILENT(-1),

    @EnumValue("LOW")
    LEVEL_LOW(5),

    @EnumValue("NORMAL")
    LEVEL_NORMAL(25),

    @EnumValue("HIGH")
    LEVEL_HIGH(70),

    @EnumValue("CRITICAL")
    LEVEL_CRITICAL(100);

    public int priority;

    AlertLevel(int priority) {
        this.priority = priority;
    }
}
