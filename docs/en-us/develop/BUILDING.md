# Build Guide

## Prerequisites

- Install [Eclipse Temurin JDK 25](https://adoptium.net/temurin/releases?version=25)

- Install [Android Studio](https://developer.android.com/studio)

- Download the MAA Core prebuilt artifacts (.so libraries + resource files)

  ```bash
  python scripts/setup_maa_core.py
  ```

## Build Steps

- Open this folder in Android Studio. Under Settings - Build, Execution, Deployment - Build Tools - Gradle - Gradle Projects - Gradle JDK, select the temurin-25 you installed earlier.

- Run "Sync Project with Gradle Files". Android Studio will install the remaining dependencies automatically. Once finished, run the "Assemble app" Run Configuration to build the APK.

## Installing Updates with the Same Signature

GitHub Actions uses the same release signing key for `main`, `dev`, and Release builds. To replace an Actions or Release installation directly from Android Studio, configure the same key in the uncommitted `local.properties` file:

```properties
KEYSTORE_PATH=/absolute/path/to/release.jks
KEYSTORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

With these values configured, both Debug and Release variants use that key. Local Debug and pull request builds fall back to the standard Android debug key when no release key is configured.
