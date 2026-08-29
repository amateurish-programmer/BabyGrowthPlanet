plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

apply(plugin = "kotlin-kapt")

configure<org.jetbrains.kotlin.gradle.plugin.KaptExtension> {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

base {
    archivesName.set("BaoBeiGrowthPlanet-v1.4.1")
}

android {
    namespace = "com.baobei.growthplanet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.baobei.growthplanet"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "1.4.1"
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    add("kapt", "androidx.room:room-compiler:2.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}




// Gradle 8/JUnit on Windows can misread non-ASCII project paths in its Java argfile.
// Stage only project-owned test classpath entries under the ASCII Gradle home before forking JUnit.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    doFirst {
        val stagedRoot = File(gradle.gradleUserHomeDir, "growthplanet-test-classpath/$name")
        stagedRoot.deleteRecursively()
        stagedRoot.mkdirs()
        val projectPrefix = rootProject.projectDir.canonicalPath + File.separator
        classpath = files(classpath.files.mapIndexedNotNull { index, source ->
            if (!source.exists()) null
            else if (!source.canonicalPath.startsWith(projectPrefix)) source
            else {
                val target = File(stagedRoot, "$index-${source.name}")
                if (source.isDirectory) source.copyRecursively(target, overwrite = true)
                else {
                    target.parentFile.mkdirs()
                    source.copyTo(target, overwrite = true)
                }
                target
            }
        })
    }
}
