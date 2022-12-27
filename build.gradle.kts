import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
    id("org.springframework.boot") version "3.0.0"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("plugin.spring") version "1.3.61"
}

group = "org.example"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.github.microutils:kotlin-logging:1.5.3")
    implementation("com.auth0:java-jwt:3.7.0")
    implementation("com.amazonaws:aws-java-sdk-s3:1.12.353")
    implementation("software.amazon.awssdk:s3-transfer-manager:2.17.123-PREVIEW")
    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("javax.xml.ws:jaxws-api:2.3.0")
    implementation("com.sun.xml.ws:rt:2.3.1")
    implementation("javax.jws:javax.jws-api:1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.2")
    implementation("org.jetbrains.kotlinx:atomicfu:0.14.2")
    implementation("com.squareup.retrofit2:retrofit:2.8.1")
    implementation("com.squareup.retrofit2:converter-jackson:2.8.1")
    implementation("org.apache.httpcomponents:httpmime:4.5.12")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("MainKt")
}

tasks.getByName<Jar>("jar") {
    enabled = true
}