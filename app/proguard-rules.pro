# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# bridge.cpp resolves these classes and methods by their original JNI names.
-keep class com.aliothmoon.maameow.bridge.NativeBridgeLib { *; }
-keep class com.aliothmoon.maameow.maa.DriverClass { *; }

# JNA maps Java method names directly to exported MaaCore symbols.
-dontwarn java.awt.**
-keep class com.sun.jna.** { *; }
-keep interface com.aliothmoon.maameow.maa.MaaCoreLibrary { *; }
-keep interface com.aliothmoon.maameow.maa.AsstApiCallback { *; }

# Shizuku and liblauncher instantiate these entry points outside the app's
# normal call graph. Keep constructors/main methods that R8 cannot observe.
-keep class com.aliothmoon.maameow.remote.RemoteServiceImpl { *; }
-keep class com.aliothmoon.maameow.remote.LogcatCaptureServiceImpl { *; }
-keep class com.aliothmoon.maameow.root.RootServiceStarter { *; }
-keep class com.aliothmoon.maameow.root.RootUserService { *; }
