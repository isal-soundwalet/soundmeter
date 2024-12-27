// Root-level build.gradle.kts file
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3") // Pastikan versi Gradle sesuai
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0") // Pastikan versi Kotlin sesuai
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

task<Delete>("clean") {
    delete(rootProject.buildDir)
}
