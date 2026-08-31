plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "io.temporal.learn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.temporal:temporal-sdk:1.31.0")
    implementation("ch.qos.logback:logback-classic:1.5.20")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("fulfillment.FulfillmentWorkerKt")
}

tasks.register<JavaExec>("runWorker") {
    group = "application"
    description = "Runs the FulfillmentWorker"
    mainClass.set("fulfillment.FulfillmentWorkerKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.register<JavaExec>("runStarter") {
    group = "application"
    description = "Runs the Starter"
    mainClass.set("fulfillment.StarterKt")
    classpath = sourceSets["main"].runtimeClasspath
}
