plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)

    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
}

android {

    namespace = "com.PasajesYa.pasajesya"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }


    defaultConfig {

        applicationId = "com.PasajesYa.pasajesya"

        minSdk = 23

        targetSdk = 36

        versionCode = 2

        versionName = "2.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {

        release {

            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }


    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }


    buildFeatures {

        viewBinding = true
    }
}


dependencies {


    // =====================================================
    // ANDROID
    // =====================================================

    implementation(
        libs.androidx.activity.ktx
    )

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.appcompat
    )

    implementation(
        libs.material
    )

    implementation(
        libs.androidx.activity
    )

    implementation(
        libs.androidx.constraintlayout
    )


    // =====================================================
    // GOOGLE LOGIN - CREDENTIAL MANAGER
    // =====================================================

    implementation(
        libs.androidx.credentials
    )

    implementation(
        libs.androidx.credentials.play.services.auth
    )

    implementation(
        libs.googleid
    )


    // =====================================================
    // FIREBASE
    // =====================================================

    implementation(
        platform(
            "com.google.firebase:firebase-bom:34.16.0"
        )
    )


    implementation(
        "com.google.firebase:firebase-auth"
    )


    implementation(
        "com.google.firebase:firebase-firestore"
    )


    implementation(
        "com.google.firebase:firebase-storage"
    )


    // =====================================================
    // GOOGLE MAPS
    // =====================================================

    implementation(
        "com.google.android.gms:play-services-maps:20.0.0"
    )


    // =====================================================
    // UBICACIÓN GPS
    // =====================================================

    implementation(
        "com.google.android.gms:play-services-location:21.4.0"
    )


    // =====================================================
    // GLIDE
    // FOTO DE PERFIL GOOGLE
    // =====================================================

    implementation(
        "com.github.bumptech.glide:glide:4.16.0"
    )


    // =====================================================
    // INTERFAZ
    // =====================================================

    implementation(
        "androidx.core:core-splashscreen:1.2.0"
    )


    implementation(
        "androidx.recyclerview:recyclerview:1.3.2"
    )


    implementation(
        "androidx.drawerlayout:drawerlayout:1.2.0"
    )


    // =====================================================
    // TEST
    // =====================================================

    testImplementation(
        libs.junit
    )


    androidTestImplementation(
        libs.androidx.junit
    )


    androidTestImplementation(
        libs.androidx.espresso.core
    )
}


// =========================================================
// GOOGLE MAPS - SECRETS
// =========================================================

secrets {

    /*
     * Aquí está:
     *
     * MAPS_API_KEY=AIza....
     */

    propertiesFileName =
        "secrets.properties"


    /*
     * Archivo de respaldo.
     *
     * Si no tienes local.defaults.properties
     * no pasa nada para tu compilación local mientras
     * secrets.properties exista.
     */

    defaultPropertiesFileName =
        "local.properties"
}