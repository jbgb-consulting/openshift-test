/*
 * Copyright (C) 2016 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

//  Aufrufe
//  1) Microservice uebersetzen und starten
//        .\gradlew -t
//        .\gradlew compileKotlin
//        .\gradlew compileTestKotlin
//
//  2) Microservice als selbstausfuehrendes JAR erstellen und ausfuehren
//        .\gradlew bootJar
//        java -jar build/libs/....jar --spring.profiles.active=dev
//
//  3) Tests
//        .\gradlew test [--rerun-tasks] [--fail-fast] jacocoTestReport
//
//  4) Daemon abfragen und stoppen
//        .\gradlew --status
//        .\gradlew --stop
//
//  5) Verfuegbare Tasks auflisten
//        .\gradlew tasks

import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", Versions.Plugins.kotlin))
        classpath(kotlin("allopen", Versions.Plugins.allOpen))
        classpath(kotlin("noarg", Versions.Plugins.noArg))

        classpath("org.springframework.boot:spring-boot-gradle-plugin:${Versions.Plugins.springBoot}")
    }
}

plugins {
    idea
    jacoco
    `project-report`

    kotlin("jvm") version Versions.Plugins.kotlin
    id("org.jetbrains.kotlin.plugin.allopen") version Versions.Plugins.allOpen
    id("org.jetbrains.kotlin.plugin.noarg") version Versions.Plugins.noArg
    id("com.adarshr.test-logger") version Versions.Plugins.testLogger
}

apply(plugin = "org.springframework.boot")

defaultTasks = mutableListOf("bootRun")
group = "de.hska"
version = "1.0"

repositories {
    mavenCentral()
    maven("http://dl.bintray.com/kotlin/kotlin-eap")
    maven("http://repo.spring.io/libs-milestone")
    maven("http://repo.spring.io/release")

    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:${Versions.kotlin}"))
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${Versions.springBoot}"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${Versions.springCloud}"))

    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.thoughtworks.paranamer:paranamer:${Versions.paranamer}")

    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-tomcat"){
        exclude(module = "tomcat-embed-el")
        exclude(module = "tomcat-embed-websocket")
    }

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.hateoas:spring-hateoas") {
        exclude(module = "spring-plugin-core")
        exclude(module = "json-path")
    }

    implementation("org.springframework.boot:spring-boot-actuator")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.security:spring-security-rsa:${Versions.springSecurityRsa}") {
        exclude(module = "bcpkix-jdk15on")
    }

    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    runtimeOnly("org.springframework.boot:spring-boot-devtools:${Versions.springBoot}")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(module = "junit")
        exclude(module = "hamcrest-core")
        exclude(module = "hamcrest-library")
        //exclude(module = "assertj-core")
        exclude(module = "mockito-core")
        exclude(module = "json-path")
        exclude(module = "jsonassert")
        exclude(module = "xmlunit-core")
    }
    testImplementation("org.springframework.security:spring-security-test")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains" && requested.name == "annotations") {
            useVersion(Versions.annotations)
        }

        if (requested.name == "reactive-streams") {
            useVersion(Versions.reactiveStreams)
        }
        if (requested.name == "hibernate-validator") {
            useVersion(Versions.hibernateValidator)
        }
        if (requested.group == "com.fasterxml.jackson.core" || requested.group == "com.fasterxml.jackson.datatype" ||
            requested.group == "com.fasterxml.jackson.dataformat" ||
            requested.group == "com.fasterxml.jackson.module") {
            useVersion(Versions.jackson)
        }
        if (requested.name == "kotlin-reflect") {
            useVersion(Versions.kotlin)
        }
        if (requested.group == "org.apache.tomcat.embed") {
            useVersion(Versions.tomcat)
        }
    }
}

allOpen {
    annotation("org.springframework.stereotype.Component")
    annotation("org.springframework.stereotype.Service")
    annotation("org.springframework.boot.context.properties.ConfigurationProperties")
}

noArg {
    annotation("org.springframework.boot.context.properties.ConfigurationProperties")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "12"
            verbose = true
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
        dependsOn(processResources)
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "12"
            verbose = true
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    named<BootRun>("bootRun") {
        jvmArgs = ArrayList(jvmArgs).apply {
            add("--illegal-access=deny")
            add("-Dspring.profiles.active=dev")
            add("-Dspring.config.location=classpath:/bootstrap.yml,classpath:/application.yml,classpath:/application-dev.yml")
            add("-Djavax.net.ssl.trustStore=${System.getProperty("user.dir")}/src/main/resources/truststore.p12")
            add("-Djavax.net.ssl.trustStorePassword=zimmermann")
        }
    }

    named<BootJar>("bootJar") {
        doLast {
            println("")
            println("Aufruf der ausfuehrbaren JAR-Datei:")
            println("java -jar build/libs/${archiveFileName.get()} --spring.profiles.active=dev")
            println("")
        }
    }

    test {
        @Suppress("UnstableApiUsage")
        useJUnitPlatform {
            includeEngines("junit-jupiter")

            includeTags("rest", "multimediaRest", "streamingRest", "service")
        }

        systemProperty("javax.net.ssl.trustStore", "./src/main/resources/truststore.p12")
        systemProperty("javax.net.ssl.trustStorePassword", "zimmermann")
        systemProperty("junit.platform.output.capture.stdout", true)
        systemProperty("junit.platform.output.capture.stderr", true)

        finalizedBy(jacocoTestReport)
    }

    getByName<JacocoReport>("jacocoTestReport") {
        reports {
            @Suppress("UnstableApiUsage")
            xml.isEnabled = true
            @Suppress("UnstableApiUsage")
            html.isEnabled = true
        }
        afterEvaluate {
            classDirectories.setFrom(files(classDirectories.files.map {
                fileTree(it) { exclude("**/config/**", "**/entity/**") }
            }))
        }
    }

    idea {
        module {
            isDownloadJavadoc = true
        }
    }
}
