plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "br.com.wdc.shopping.view.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.wdc.shopping.view.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
        }
    }
}

configurations.all {
    exclude(group = "ch.qos.logback", module = "logback-core")
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "br.com.wdc.shopping", module = "br.com.wdc.shopping.presentation.shared")
}

dependencies {
    // Kotlin
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:2.1.0"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // WeDoCode Framework (from mavenLocal)
    implementation("br.com.wdc.framework:br.com.wdc.framework.commons:1.0.0")
    implementation("br.com.wdc.framework:br.com.wdc.framework.cube:2.0.0")

    // WeDoCode Shopping (from mavenLocal)
    implementation("br.com.wdc.shopping:br.com.wdc.shopping.domain:1.0.0")
    implementation("br.com.wdc.shopping:br.com.wdc.shopping.presentation:1.0.0")

    // REST client library (from mavenLocal)
    implementation("br.com.wdc.shopping:br.com.wdc.shopping.api-client:1.0.0")

    // Logging (SLF4J + Android-compatible backend)
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("com.github.tony19:logback-android:3.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
