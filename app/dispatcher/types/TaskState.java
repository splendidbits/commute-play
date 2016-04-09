package dispatcher.types;

import com.avaje.ebean.annotation.EnumValue;

/**
 * An enum defining a possible state of a Task.
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/7/16 Splendid Bits.
 */
public enum TaskState {
    @EnumValue("IDLE")
    STATE_IDLE,
    @EnumValue("PROCESSING")
    STATE_PROCESSING,
    @EnumValue("FAILED")
    STATE_FAILED,
    @EnumValue("STATE_PARTIALLY_PROCESSED")
    STATE_PARTIALLY_PROCESSED,
    @EnumValue("COMPLETE")
    STATE_COMPLETE
}