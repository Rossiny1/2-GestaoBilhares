# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class **_HiltModules.** { *; }
-keep class **Hilt_** { *; }
-keep class javax.inject.** { *; }
-keepattributes *Annotation*

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Room
-keep class androidx.room.** { *; }
-keep class com.example.gestaobilhares.data.entities.** { *; }
-keep class com.example.gestaobilhares.data.dao.** { *; }

# App models (Gson)
-keep class com.example.gestaobilhares.** { *; }
-dontwarn sun.misc.**

# Navigation Safe Args
-keep class **Directions$* { *; }
-keep class **Args { *; }

# iText warnings
-dontwarn org.bouncycastle.**
-dontwarn org.spongycastle.**