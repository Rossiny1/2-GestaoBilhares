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

# Gson - CRITICAL: Preserve generic signatures for TypeToken
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Gson TypeToken - Fix for ExceptionInInitializerError
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ✅ CORREÇÃO CRÍTICA: Preservar TypeToken específico do SyncRepository
# Isso garante que as assinaturas genéricas sejam preservadas após otimização R8/ProGuard
-keep class com.example.gestaobilhares.sync.SyncRepository$Companion$MapTypeToken { *; }

# Generic signatures for all app classes (needed for Gson)
-keepattributes Signature,InnerClasses,EnclosingMethod

# App models (Gson)
-keep class com.example.gestaobilhares.** { *; }
-dontwarn sun.misc.**

# Navigation Safe Args
-keep class **Directions$* { *; }
-keep class **Args { *; }

# iText warnings
-dontwarn org.bouncycastle.**
-dontwarn org.spongycastle.**
-dontwarn org.slf4j.**