import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlinx.serialization)
}

group = "org.simpmusic"
version = libs.versions.appversion.get()

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.add("-Xwhen-guards")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.security.test)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // AppWrite
    implementation(libs.appwrite.sdk)

    implementation(libs.spring.swagger)

    // Rate Limiting
    implementation(libs.bucket4j.spring.boot.starter)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.javax.cache.api)
    implementation(libs.caffeine)
    implementation(libs.caffeine.jcache)

    // Validation
    implementation(libs.spring.boot.starter.validation)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    onlyIf { false } // Temporarily disable tests
}
