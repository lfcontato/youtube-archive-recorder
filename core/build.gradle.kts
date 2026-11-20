plugins {
    kotlin("jvm")
}

group = "com.youtubearchive"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}
