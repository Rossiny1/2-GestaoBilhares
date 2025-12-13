// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    // ✅ CORREÇÃO: KSP plugin no build principal
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}

buildscript {
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.7")
    }
}