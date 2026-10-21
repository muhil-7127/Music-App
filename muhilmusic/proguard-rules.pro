# Muhil Music ProGuard Rules
-keepattributes SourceFile,LineNumberTable
-keep class com.muhil.music.** { *; }
-keep class com.google.firebase.** { *; }
-keepattributes *Annotation*
-dontwarn com.google.**
-dontwarn androidx.media3.**

# Coroutines
-keepnames class kotlinx.coroutines.** { *; }

# Room SQLite & Entity reflections
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# AndroidX DataStore
-keep class androidx.datastore.** { *; }

# Coil 3 Image Loading
-keep class coil3.** { *; }
-dontwarn coil3.**

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }

# Kotlinx Serialization
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
