import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.springframework.boot") version "3.3.5"
  id("io.spring.dependency-management") version "1.1.6"
  kotlin("jvm") version "2.3.10"
  kotlin("plugin.spring") version "2.3.10"
}

group = "com.example"
version = "0.1.0"

java {
  toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories { mavenCentral() }

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")

  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
  }
  testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
  testImplementation("io.kotest:kotest-assertions-core:5.9.1")
  testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
  testImplementation("io.mockk:mockk:1.13.12")

  testImplementation("org.testcontainers:junit-jupiter:1.20.3")
}

tasks.withType<Test> { useJUnitPlatform() }


tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    freeCompilerArgs.add("-Xjsr305=strict")
    jvmTarget.set(JvmTarget.JVM_21)
  }
}

val integrationTest by sourceSets.creating {
  kotlin.srcDir("src/integrationTest/kotlin")
  resources.srcDir("src/integrationTest/resources")
  compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output + configurations.testRuntimeClasspath.get()
  runtimeClasspath += output + compileClasspath
}

configurations.getByName("integrationTestImplementation").extendsFrom(configurations.testImplementation.get())
configurations.getByName("integrationTestRuntimeOnly").extendsFrom(configurations.testRuntimeOnly.get())

tasks.register<Test>("integrationTest") {
  description = "Runs integration tests."
  group = "verification"
  testClassesDirs = integrationTest.output.classesDirs
  classpath = integrationTest.runtimeClasspath
  shouldRunAfter("test")
  useJUnitPlatform()
  onlyIf {
    val dockerHost = System.getenv("DOCKER_HOST")
    val hasDockerSocket = file("/var/run/docker.sock").exists()
    dockerHost != null || hasDockerSocket
  }
}

tasks.named("check") {
  dependsOn("integrationTest")
}
