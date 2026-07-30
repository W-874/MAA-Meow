# Third-Party Notices

MaaNyan is based on the upstream [Aliothmoon/MAA-Meow](https://github.com/Aliothmoon/MAA-Meow), created by Aliothmoon.

The following projects are distributed with or used by MaaNyan under their original licenses:

| Project | License | Use in MaaNyan |
| --- | --- | --- |
| [MaaCore / MaaAssistantArknights](https://github.com/MaaAssistantArknights/MaaAssistantArknights) | AGPL-3.0 | Task-execution core; the native library and resources are obtained by the setup flow and loaded at runtime through JNA. |
| [scrcpy](https://github.com/Genymobile/scrcpy) | Apache License 2.0 | Adapted Android system-service, virtual-display, and input reflection wrappers in `app/src/main/java/com/aliothmoon/maameow/third/`. |
| [Shizuku](https://github.com/RikkaApps/Shizuku) | Apache License 2.0 | Privileged user-service API and provider. |
| [JNA](https://github.com/java-native-access/jna) | Apache License 2.0 | Java/Kotlin bridge to the MaaCore native interface. |
| [AndroidX / Jetpack Compose](https://developer.android.com/jetpack) | Apache License 2.0 | Android compatibility, lifecycle, navigation, UI, and Material components. |
| [Kotlin](https://github.com/JetBrains/kotlin) | Apache License 2.0 | Application language and Kotlin runtime libraries. |
| [Koin](https://github.com/InsertKoinIO/koin) | Apache License 2.0 | Dependency injection for Android and Compose. |
| [OkHttp](https://github.com/square/okhttp) | Apache License 2.0 | Network downloads and HTTP requests. |
| [Timber](https://github.com/JakeWharton/timber) | Apache License 2.0 | Android logging. |
| [fastjson2](https://github.com/alibaba/fastjson2) | See upstream license | JSON parsing. |
| Focus API (`com.xzakota.hyper.notification:focus-api`) | See upstream license | Notification focus API. |
| [DeviceCompat](https://github.com/getActivity/DeviceCompat) / [XXPermissions](https://github.com/getActivity/XXPermissions) | See upstream license | Device compatibility and runtime permissions. |
| [FloatingX](https://github.com/petterp/floatingx) / [Sonner](https://github.com/dokar3/sonner) | See upstream license | Floating-window and toast UI. |
| [Eclipse Angus](https://eclipse-ee4j.github.io/angus-mail/) / Jakarta Activation API | See upstream license | SMTP mail and attachment handling. |
| [Compose Markdown](https://github.com/Jeziellago/compose-markdown) / [Reorderable](https://github.com/Calvin-LL/Reorderable) | See upstream license | Markdown rendering and reorderable UI. |
| [Sora Editor](https://github.com/Rosemoe/sora-editor) / [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) | See upstream license | Text editing and serialization. |

This page is an offline summary and does not replace upstream license texts or add copyright claims. The complete set of dependency licenses and notices, including transitive dependencies, is authoritative in the APK's `META-INF` metadata (when included by the build) and in each upstream distribution.
