package pushservices.enums;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/5/16 Splendid Bits.
 */
public enum PlatformFailureType {
    ERROR_MISSING_REG_TOKEN("MissingRegistration", false),
    ERROR_INVALID_REGISTRATION("InvalidRegistration", false),
    ERROR_NOT_REGISTERED("NotRegistered", false),
    ERROR_INVALID_PACKAGE_NAME("InvalidPackageName", true),
    ERROR_MISMATCHED_SENDER_ID("MismatchSenderId", true),
    ERROR_MESSAGE_TO_BIG("MessageTooBig", true),
    ERROR_INVALID_DATA("InvalidDataKey", true),
    ERROR_INVALID_TTL("InvalidTtl", true),
    ERROR_TOO_MANY_RETRIES("Too many retries", true),
    ERROR_EXCEEDED_MESSAGE_LIMIT("DeviceMessageRate Exceeded", true);

    public String friendlyError = null;
    public boolean isCritical = false;
    PlatformFailureType(String value, boolean critical) {
        friendlyError = value;
        isCritical = critical;
    }
}