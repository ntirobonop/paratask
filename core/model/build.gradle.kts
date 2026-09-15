plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.ntirobonop.paratask.core.model"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    testImplementation(libs.junit4)
}
