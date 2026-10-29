// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false

    // Declara el plugin KSP sin aplicarlo al proyecto raíz.
    alias(libs.plugins.ksp) apply false
}