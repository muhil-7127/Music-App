# Muhil Music ProGuard Rules
-keepattributes SourceFile,LineNumberTable
-keep class com.muhil.music.** { *; }
-keep class com.google.firebase.** { *; }
-keepattributes *Annotation*
-dontwarn com.google.**
-dontwarn androidx.media3.**
