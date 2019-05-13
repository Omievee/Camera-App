plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("android.extensions")
}


android {
    compileSdkVersion(28)
    defaultConfig {
        applicationId = "com.overtime.camera"
        minSdkVersion(26)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    dataBinding {
        isEnabled = true
    }

    dataBinding {
        isEnabled = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

        }

    }
    androidExtensions {
        isExperimental = true
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.3.21")
    implementation("com.android.support:appcompat-v7:28.0.0")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    //RecyclerView
    implementation("com.android.support:recyclerview-v7:28.0.0")
    //ConstraintLayouts
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    //RxJava
    implementation("io.reactivex.rxjava2:rxjava:2.2.8")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    //Dagger Android / Core (2.16 to work)
    implementation("com.google.dagger:dagger-android:2.16")
    implementation("com.google.dagger:dagger-android-support:2.16")
    implementation("com.google.dagger:dagger:2.16")
    implementation("androidx.appcompat:appcompat:1.0.2")
    kapt("com.google.dagger:dagger-compiler:2.16")
    kapt("com.google.dagger:dagger-android-processor:2.16")
    implementation("com.google.dagger:dagger:2.16")
    kapt("com.google.dagger:dagger-compiler:2.16")
    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.5.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.5.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.5.0")
    //Moshi
    implementation("com.squareup.moshi:moshi:1.8.0")
    implementation("com.squareup.moshi:moshi-adapters:1.8.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.8.0")
    //OKHTTP
    implementation("com.squareup.okhttp3:okhttp:3.14.1")
    implementation("com.squareup.okhttp3:logging-interceptor:3.14.1")
    //FireBase
    implementation("com.google.firebase:firebase-core:16.0.1")
    //MixPanel
    implementation("com.mixpanel.android:mixpanel-android:5.+")
    implementation("com.google.firebase:firebase-messaging:17.3.4")
    //Fabric
    implementation("com.crashlytics.sdk.android:crashlytics:2.9.9@aar") {
        isTransitive = true
    }
    //mvvm lifecycle
    implementation("android.arch.lifecycle:extensions:1.1.1")
    kapt("android.arch.lifecycle:compiler:1.1.1")
    //Fresco
    implementation("com.facebook.fresco:fresco:1.13.0")
    //Testing
    testImplementation("junit:junit:4.12")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("com.android.support.test.espresso:espresso-core:3.0.2")
}
