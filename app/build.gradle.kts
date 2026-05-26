plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.sqldelight)
  id("com.google.gms.google-services")
}

sqldelight {
  databases {
    create("DrawShareDb") {
      packageName.set("com.example.data")
    }
  }
}

kotlin {
  androidTarget {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
      freeCompilerArgs.add("-Xexpect-actual-classes")
    }
  }

  // iOS targets — declare now so SQLDelight/GitLive can target them;
  // actual iOS compilation requires Xcode (added in the next step).
  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64(),
  ).forEach { target ->
    target.binaries.framework {
      baseName = "DrawShare"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.datetime)
      implementation(libs.sqldelight.coroutines.extensions)
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)
      implementation(libs.androidx.lifecycle.viewmodel.compose)
    }

    androidMain {
      // All existing Android sources stay in place during migration.
      kotlin.srcDirs("src/main/java")
      dependencies {
        implementation(libs.sqldelight.android.driver)
        implementation(libs.firebase.firestore)
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.compose.material.icons.core)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.compose)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.lifecycle.viewmodel.compose)
        implementation(libs.androidx.glance.appwidget)
        implementation(libs.kotlinx.coroutines.android)
      }
    }

    iosMain.dependencies {
      implementation(libs.sqldelight.native.driver)
    }

    getByName("androidUnitTest") {
      kotlin.srcDirs("src/test/java")
    }

    getByName("androidInstrumentedTest") {
      kotlin.srcDirs("src/androidTest/java")
    }
  }
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.drawshare.vqxntq"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions { unitTests { isIncludeAndroidResources = true } }
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// BOMs and test/debug deps live here — platform() in KMP source set blocks
// resolves to the deprecated Any overload, so we keep them at module level.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  add("kspAndroid", libs.androidx.room.compiler)
}
