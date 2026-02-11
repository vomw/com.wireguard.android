@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val pkg: String = providers.gradleProperty("wireguardPackageName").get()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    compileSdk = 36
    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
    }
    namespace = pkg
    defaultConfig {
        applicationId = pkg
        val baseMinSdk = 24
        minSdk = (project.findProperty("targetMinSdk") as String?)?.toInt() ?: baseMinSdk
        targetSdk = 36
        val baseVersionCode = providers.gradleProperty("wireguardVersionCode").get().toInt()
        versionName = providers.gradleProperty("wireguardVersionName").get()
        buildConfigField("int", "MIN_SDK_VERSION", minSdk.toString())

        val targetAbi: String? by project
        versionCode = baseVersionCode * 10 + when (targetAbi) {
            "x86" -> 1
            "x86_64" -> 2
            "armeabi-v7a" -> 3
            "arm64-v8a" -> 4
            else -> 0
        }

        if (targetAbi != null) {
            ndk {
                abiFilters.set(listOf(targetAbi))
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-android-optimize.txt")
            packaging {
                resources {
                    excludes += "DebugProbesKt.bin"
                    excludes += "kotlin-tooling-metadata.json"
                    excludes += "META-INF/*.version"
                }
                jniLibs {
                    val targetAbi: String? by project
                    if (targetAbi != null) {
                        val allAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                        excludes += allAbis.filter { it != targetAbi }.map { "lib/$it/*" }
                    }
                }
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            packaging {
                jniLibs {
                    val targetAbi: String? by project
                    if (targetAbi != null) {
                        val allAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                        excludes += allAbis.filter { it != targetAbi }.map { "lib/$it/*" }
                    }
                }
            }
        }
        create("googleplay") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            packaging {
                jniLibs {
                    val targetAbi: String? by project
                    if (targetAbi != null) {
                        val allAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
                        excludes += allAbis.filter { it != targetAbi }.map { "lib/$it/*" }
                    }
                }
            }
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    lint {
        disable += "LongLogTag"
        warning += "MissingTranslation"
        warning += "ImpliedQuantity"
    }
}

dependencies {
    implementation(project(":tunnel"))
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.material)
    implementation(libs.zxing.android.embedded)
    implementation(libs.kotlinx.coroutines.android)
    coreLibraryDesugaring(libs.desugarJdkLibs)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:unchecked")
    options.isDeprecation = true
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget = JvmTarget.JVM_17
}
