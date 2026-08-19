plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.bondarenko.fujirecipes"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.bondarenko.fujirecipes"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /**
     * The debug key is **checked in**, and that is the point.
     *
     * Without one, AGP generates `~/.android/debug.keystore` on whatever machine is building
     * — so every CI runner signs with a different key, and Android refuses to install one
     * build over another: "App not installed", or `INSTALL_FAILED_UPDATE_INCOMPATIBLE`,
     * unless you uninstall first. One committed key makes every build an update of the last,
     * whoever produced it.
     *
     * It carries the conventional debug credentials (`androiddebugkey` / `android`), so it
     * behaves exactly like the one the SDK would have made. **It signs debug builds only.**
     * A release key is a different thing and does not belong in a repository.
     */
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        create("release") {
            val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
                ?: (project.findProperty("RELEASE_KEYSTORE_PATH") as? String)
            val keystoreFile = keystorePath?.let { file(it) }

            if (keystoreFile != null && keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                    ?: (project.findProperty("RELEASE_KEYSTORE_PASSWORD") as? String)
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                    ?: (project.findProperty("RELEASE_KEY_ALIAS") as? String)
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                    ?: (project.findProperty("RELEASE_KEY_PASSWORD") as? String)
            } else {
                // Fallback for local release builds: sign with debug key so APK is installable
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }

        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // Off by default from AGP 8. The settings screen shows the version name, which is
        // the only thing read from it.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
