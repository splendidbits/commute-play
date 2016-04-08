package models.app;

import com.avaje.ebean.annotation.EnumValue;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/7/16 Splendid Bits.
 */
public enum ProcessState {
    @EnumValue("IDLE")
    STATE_IDLE,
    @EnumValue("PROCESSING")
    STATE_PROCESSING,
    @EnumValue("FAILED")
    STATE_FAILED,
    @EnumValue("WAITING_RETRY")
    STATE_WAITING_RETRY,
    @EnumValue("COMPLETE")
    STATE_COMPLETE
}