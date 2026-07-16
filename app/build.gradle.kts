plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val generatedEcoWasteDir = layout.buildDirectory.dir("generated/ecoWaste/kotlin")
val generateEcoWasteSource = tasks.register("generateEcoWasteSource") {
    inputs.dir(rootProject.layout.projectDirectory.dir("tmp/eco_parts"))
    outputs.dir(generatedEcoWasteDir)

    doLast {
        val partsDirectory = rootProject.file("tmp/eco_parts")
        val parts = partsDirectory.listFiles()
            ?.filter { it.name.startsWith("part_") }
            ?.sortedBy { it.name }
            .orEmpty()
        require(parts.isNotEmpty()) { "Eco Waste source parts were not found." }

        val outputFile = generatedEcoWasteDir.get()
            .file("com/ahmed/yawmeyaty/EcoWasteActivity.kt")
            .asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(parts.joinToString(separator = "") { it.readText() })
    }
}

android {
    namespace = "com.ahmed.yawmeyaty"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ahmed.yawmeyaty"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "4.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main").java.srcDir(generatedEcoWasteDir.get().asFile)
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.matching { task ->
    task.name.startsWith("compile") && task.name.endsWith("Kotlin")
}.configureEach {
    dependsOn(generateEcoWasteSource)
}

dependencies {
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
