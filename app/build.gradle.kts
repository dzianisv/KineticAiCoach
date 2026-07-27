import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
  alias(libs.plugins.firebase.crashlytics)
}

// Release signing: read from a gitignored `keystore.properties` file at the repo root
// (see keystore.properties.template) or fall back to env vars (KEYSTORE_PATH,
// KEYSTORE_STORE_PASSWORD, KEYSTORE_KEY_ALIAS, KEYSTORE_KEY_PASSWORD). Never hardcode secrets.
// If neither source is available, the release build type is left unsigned so `debug` builds
// and CI checks keep working for contributors who don't have upload-key credentials.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
  if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use { load(it) }
  }
}

fun releaseSigningProperty(propertyKey: String, envKey: String): String? =
  keystoreProperties.getProperty(propertyKey) ?: System.getenv(envKey)

val releaseStoreFilePath = releaseSigningProperty("storeFile", "KEYSTORE_PATH")
val releaseStorePassword = releaseSigningProperty("storePassword", "KEYSTORE_STORE_PASSWORD")
val releaseKeyAlias = releaseSigningProperty("keyAlias", "KEYSTORE_KEY_ALIAS")
val releaseKeyPassword = releaseSigningProperty("keyPassword", "KEYSTORE_KEY_PASSWORD")
val hasReleaseSigningConfig =
  releaseStoreFilePath != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.aicoach.vtzrkm"
    minSdk = 24
    targetSdk = 36
    versionCode = 2
    versionName = "1.0.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    if (hasReleaseSigningConfig) {
      create("release") {
        storeFile = file(releaseStoreFilePath!!)
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
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
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Falls back to unsigned (no signingConfig) when keystore.properties/env vars are
      // absent, so `./gradlew :app:bundleRelease` still exercises R8/shrinking for anyone.
      signingConfig = if (hasReleaseSigningConfig) signingConfigs.getByName("release") else null
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
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

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// ---------------------------------------------------------------------------
// Release config guard — DO NOT REMOVE.
//
// The Secrets Gradle Plugin silently falls back to `.env.example` for any key
// missing from `.env`. `.env.example` ships
// `FIREBASE_PROXY_URL=https://us-central1-your-project-id.cloudfunctions.net/`,
// so a build without a real `.env` bakes that placeholder into
// `BuildConfig.FIREBASE_PROXY_URL`. `GeminiApiClient.proxyService` then
// evaluates to null (app/src/main/java/com/example/network/GeminiApiClient.kt),
// `analyzeFrame()` returns null, and the shipped app has NO working AI at all —
// no rep counting, no form feedback — while the build stays green.
//
// This happened for real: the v1.0.1 APK attached to the GitHub release by
// `.github/workflows/release-apk.yml` contained the placeholder URL. Fail the
// build instead of shipping a silently dead app.
// ---------------------------------------------------------------------------
val placeholderProxyMarker = "your-project-id"

val resolvedFirebaseProxyUrl: String =
  listOf(rootProject.file(".env"), rootProject.file(".env.example"))
    .asSequence()
    .filter { it.exists() }
    .mapNotNull { candidate ->
      Properties()
        .apply { FileInputStream(candidate).use { load(it) } }
        .getProperty("FIREBASE_PROXY_URL")
    }
    .firstOrNull()
    ?.trim()
    .orEmpty()

val verifyReleaseSecrets =
  tasks.register("verifyReleaseSecrets") {
    group = "verification"
    description =
      "Fails the build when FIREBASE_PROXY_URL is empty or is still the .env.example placeholder."
    doLast {
      if (resolvedFirebaseProxyUrl.isEmpty()) {
        throw GradleException(
          "FIREBASE_PROXY_URL is empty. The release build would ship with no AI backend. " +
            "Create a `.env` at the repo root with FIREBASE_PROXY_URL=<your Cloud Function base URL> " +
            "(value lives in Bitwarden, collection 'dev'). In CI, set the FIREBASE_PROXY_URL repo secret."
        )
      }
      if (resolvedFirebaseProxyUrl.contains(placeholderProxyMarker)) {
        throw GradleException(
          "FIREBASE_PROXY_URL still contains the '$placeholderProxyMarker' placeholder from .env.example. " +
            "Shipping this produces an app with NO working AI (GeminiApiClient.proxyService would be null). " +
            "Provide the real Cloud Function base URL via `.env` locally or the FIREBASE_PROXY_URL repo secret in CI."
        )
      }
    }
  }

// `preReleaseBuild` is AGP's lifecycle anchor for the release variant, so this
// fails fast — before compiling — for assembleRelease / bundleRelease / any
// other release-variant task.
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(verifyReleaseSecrets) }

// Belt-and-braces: keep the guard attached even if AGP renames the anchor.
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
  dependsOn(verifyReleaseSecrets)
}

// ERROR (not WARN): a release build with no `app/google-services.json` silently
// drops google_app_id / default_web_client_id, which breaks Firebase Auth at
// runtime. Never let that pass as a warning.
googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.ERROR }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  implementation(libs.firebase.firestore)
  implementation(libs.billing.ktx)
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.crashlytics)
  implementation(libs.firebase.config)
  implementation(libs.firebase.functions)

  // Firebase Auth with Google Sign-In requires all of the following to be uncommented together.
  // If you are using Firebase Auth with other providers (e.g. Email/Password), you may only need
  // firebase-auth.
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.mlkit.pose.detection)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
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
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
