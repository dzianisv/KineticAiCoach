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

# --- Conservative keep rules for release (R8/minify + shrinkResources) ---
# Kept intentionally broad rather than fully hand-tuned: correctness (no runtime crashes from
# stripped reflection-based code) is prioritized over squeezing out the last few KB.

-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes SourceFile,LineNumberTable

# ML Kit (pose detection) — most keep rules ship in the AAR's own consumer-rules, but pin the
# public API + native/internal glue explicitly since these are loaded reflectively/via JNI.
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.android.odml.** { *; }
-dontwarn com.google.android.odml.**

# Firebase (Auth, Firestore, AppCheck, AI/Vertex) — Firestore in particular reflects over POJO
# getters/setters/fields, so keep annotations and our own model classes below.
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepclassmembers class com.google.firebase.firestore.** { *; }

# CameraX — reflection-free in normal use, but keep the public surface since camera providers
# are resolved via service discovery / reflection at startup.
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# kotlinx.coroutines — standard rules to avoid stripping internals R8 can't otherwise see used.
-dontwarn kotlinx.coroutines.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Moshi (JSON) — keep classes annotated with @JsonClass and their generated adapters, plus
# @FromJson/@ToJson custom adapter methods, per Moshi's official R8 guidance.
-keepclasseswithmembers class * {
    @com.squareup.moshi.FromJson <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.ToJson <methods>;
}
-keep @com.squareup.moshi.JsonQualifier interface *
-keepnames @com.squareup.moshi.JsonClass class *
-if @com.squareup.moshi.JsonClass class *
-keep class <1>JsonAdapter {
    <init>(...);
    <fields>;
}
-if @com.squareup.moshi.JsonClass class * extends java.lang.Enum
-keepnames class <1>

# Retrofit / OkHttp — standard rules for the reflective/annotation-based bits.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# org.json — platform classes (android.jar), not shrunk by R8, but keep any direct references
# from obfuscation-sensitive call sites just in case a future refactor moves this into app code.
-dontwarn org.json.**

# App network/data model classes — these are (de)serialized via Moshi/Retrofit, read/written by
# Room, and synced through Firestore, all of which rely on field/constructor reflection.
-keep class com.example.network.** { *; }
-keep class com.example.data.** { *; }
-keepclassmembers class com.example.network.** { *; }
-keepclassmembers class com.example.data.** { *; }

# Room — entities/DAOs are annotation-processed at compile time (KSP), but keep entity classes
# so field names used for SQL column mapping survive.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class *

# Kotlin metadata/reflection support used by Moshi's Kotlin codegen and Compose.
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-dontwarn kotlin.reflect.**
-keep class kotlin.reflect.** { *; }
