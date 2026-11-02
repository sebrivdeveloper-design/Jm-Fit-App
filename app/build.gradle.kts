plugins {
    alias(libs.plugins.android.application)

    // Aplica el plugin KSP para generar código durante la compilación
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.forcetech.jmfitapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.forcetech.jmfitapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // ViewBinding: acceso tipado a las vistas de cada layout (FragmentLoginBinding, etc.)
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    implementation(libs.androidx.core.splashscreen)

    // Dependencias de Room para la base de datos local
    implementation(libs.androidx.room.runtime) // Funcionalidad principal de Room
    implementation(libs.androidx.room.ktx) // Extensiones de Kotlin y corrutinas
    ksp(libs.androidx.room.compiler) // Genera automáticamente el código de Room

    // Navegación entre pantallas (un subgrafo por rol)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Fragments, ViewModel y corrutinas atadas al ciclo de vida
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    // Listas (rutinas, productos, etc.)
    implementation(libs.androidx.recyclerview)
}
