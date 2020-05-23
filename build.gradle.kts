val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val postgres_version: String by project
val hikaricp_version: String by project

plugins {
    application
    kotlin("jvm") version "1.3.70"

    // FatJAR packaging plugin
    id("com.github.johnrengelman.shadow") version "5.0.0"

    // Linter
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

group = "dev.simonestefani"
version = "0.0.1"

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenLocal()
    jcenter()
    maven { url = uri("https://kotlin.bintray.com/ktor") }
}

dependencies {
    // BOMs
    implementation(platform("io.ktor:ktor-bom:$ktor_version"))

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")

    // Ktor
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-locations")
    implementation("io.ktor:ktor-server-sessions")
    implementation("io.ktor:ktor-auth")
    implementation("io.ktor:ktor-auth-jwt")
    implementation("io.ktor:ktor-gson")

    // Persistance
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.postgresql:postgresql:$postgres_version")
    implementation("com.zaxxer:HikariCP:$hikaricp_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Testing
    testImplementation("io.ktor:ktor-server-tests")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf("Main-Class" to application.mainClassName)
        )
    }
}
