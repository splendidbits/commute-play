package interfaces;

public interface PlatformMessage {
    enum PlatformType {
        PLATFORM_TYPE_GCM,
        PLATFORM_TYPE_APNS;
    }

    PlatformType getPlatformType();
}
