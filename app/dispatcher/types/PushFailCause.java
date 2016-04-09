package dispatcher.types;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/8/16 Splendid Bits.
 */
public enum PushFailCause {
    MISSING_CREDENTIALS,
    ENDPOINT_TIMEOUT,
    AUTH_ERROR,
    GCM_MESSAGE_JSON_ERROR,
    MISSING_RECIPIENTS,
    UNKNOWN_ERROR
}