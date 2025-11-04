// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Plugins definidos via Version Catalog (já existiam)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

    // Plugin do Google Services para Firebase
    id("com.google.gms.google-services") version "4.4.2" apply false
}