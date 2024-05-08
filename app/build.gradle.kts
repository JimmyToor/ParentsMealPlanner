plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.22"
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.jimmy.parentsmealplanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.jimmy.parentsmealplanner"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    // Variables for versions
    val composeBomVersion = "2024.04.01"
    val composeVersion = "1.6.6"
    val daggerVersion = "2.51.1"
    val hiltVersion = "1.2.0"
    val junitVersion = "4.13.2"
    val roomVersion = "2.6.1"
    val navigationVersion = "2.7.7"
    val lifecycleVersion = "2.7.0"
    val mockkVersion = "1.13.10"

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.navigation:navigation-compose:$navigationVersion")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    implementation("com.google.dagger:hilt-android:$daggerVersion")
    implementation("androidx.hilt:hilt-navigation-compose:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-fragment:$hiltVersion")
    implementation("androidx.hilt:hilt-work:$hiltVersion")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.hilt:hilt-navigation-fragment:$hiltVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")
    implementation("androidx.navigation:navigation-runtime-ktx:$navigationVersion")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.navigation:navigation-compose:$navigationVersion")
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("androidx.collection:collection-ktx:1.4.0")
    implementation("androidx.compose.animation:animation:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.compose.material:material-icons-core:$composeVersion")
    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.navigation:navigation-common:$navigationVersion")
    implementation("androidx.navigation:navigation-runtime-ktx:$navigationVersion")
    implementation("androidx.room:room-common:$roomVersion")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
    implementation("com.google.dagger:dagger:$daggerVersion")
    implementation("com.google.dagger:hilt-core:$daggerVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("androidx.datastore:datastore-preferences:1.1.0")
    implementation("io.coil-kt:coil-compose:2.6.0")

    ksp("androidx.room:room-compiler:$roomVersion")
    ksp("com.google.dagger:hilt-android-compiler:$daggerVersion")
    ksp("androidx.hilt:hilt-compiler:$hiltVersion")

    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("junit:junit:$junitVersion")
    testImplementation("androidx.test.ext:junit-ktx:1.1.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("io.mockk:mockk-android:$mockkVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")

    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("junit:junit:$junitVersion")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestRuntimeOnly("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:monitor:1.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugRuntimeOnly("androidx.compose.ui:ui-test-manifest:1.6.6")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
