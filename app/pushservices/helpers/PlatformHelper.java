package pushservices.helpers;

import pushservices.enums.FailureType;

import javax.annotation.Nonnull;

/**
 * Static methods for commonly used GCM Platform functions.
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/11/16 Splendid Bits.
 */
public class PlatformHelper {

    /**
     * Get a GCM Response failure type for a given GCM error.
     * @param error GCM response error.
     *
     * @return failureType error for Message or Recipient.
     */
    public static FailureType getGcmFailureType(@Nonnull String error) {
        FailureType failureType;

        switch (error) {
            case "MissingRegistration":
                failureType = FailureType.MESSAGE_REGISTRATIONS_MISSING;
                break;
            case "InvalidRegistration":
                failureType = FailureType.RECIPIENT_REGISTRATION_INVALID;
                break;
            case "NotRegistered":
                failureType = FailureType.RECIPIENT_NOT_REGISTERED;
                break;
            case "DeviceMessageRate Exceeded":
                failureType = FailureType.RECIPIENT_RATE_EXCEEDED;
                break;
            case "InvalidPackageName":
                failureType = FailureType.MESSAGE_PACKAGE_INVALID;
                break;
            case "MismatchSenderId":
                failureType = FailureType.PLATFORM_AUTH_MISMATCHED;
                break;
            case "MessageTooBig":
                failureType = FailureType.MESSAGE_TOO_LARGE;
                break;
            case "InvalidDataKey":
                failureType = FailureType.MESSAGE_PAYLOAD_INVALID;
                break;
            case "InvalidTtl":
                failureType = FailureType.MESSAGE_TTL_INVALID;
                break;
            case "DeviceMessageRate":
                failureType = FailureType.PLATFORM_LIMIT_EXCEEDED;
                break;
            case "Unavailable":
                failureType = FailureType.PLATFORM_UNAVAILABLE;
                break;
            default:
                failureType = FailureType.ERROR_UNKNOWN;
                break;
        }
        return failureType;
    }
}
