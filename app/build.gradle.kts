plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.budget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.budget"
        minSdk = 26
        //noinspection OldTargetApi
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        viewBinding = true
    }
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    //noinspection GradleDependency
    implementation("androidx.core:core-ktx:1.9.0")
    //noinspection GradleDependency
    implementation("androidx.appcompat:appcompat:1.6.1")
    //noinspection GradleDependency
    implementation("com.google.android.material:material:1.11.0")
    //noinspection GradleDependency
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.databinding:databinding-adapters:8.9.1")
    testImplementation("junit:junit:4.13.2")
    //noinspection GradleDependency
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    //noinspection GradleDependency
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.itextpdf:itext7-core:7.2.3")
    //noinspection GradleDependency
    implementation("androidx.room:room-runtime:2.5.0")
    //noinspection GradleDependency
    ksp("androidx.room:room-compiler:2.5.0")
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    implementation("org.apache.xmlbeans:xmlbeans:5.1.1")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    //noinspection GradleDependency
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.slf4j:slf4j-simple:1.7.32")
    //noinspection GradleDependency
    implementation ("androidx.activity:activity-ktx:1.7.1")
    //noinspection GradleDependency
    implementation ("androidx.fragment:fragment-ktx:1.5.7")
    //noinspection GradleDependency
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

}
