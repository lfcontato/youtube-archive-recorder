import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.jvm.tasks.Jar

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "com.youtubearchive"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "com.youtubearchive.desktop.MainKt"
    }
}

compose.desktop {
    application {
        mainClass = "com.youtubearchive.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "YouTubeArchiveRecorder"
            packageVersion = "0.1.0"
            windows {
                shortcut = true
                menu = true
                dirChooser = true
                perUserInstall = true
            }
        }
    }
}
