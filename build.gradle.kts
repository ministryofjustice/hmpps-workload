plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.1.2"
  kotlin("plugin.spring") version "1.6.10"
  kotlin("plugin.jpa") version "1.6.10"
  id("io.gitlab.arturbosch.detekt").version("1.17.1")
  id("org.jetbrains.kotlin.plugin.allopen").version("1.6.0")
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

allOpen {
  annotations("javax.persistence.Entity")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.0.2")

  implementation("org.springdoc:springdoc-openapi-ui:1.6.6")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.6")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.6")

  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  runtimeOnly("com.zaxxer:HikariCP")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.3.3")

  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.mock-server:mockserver-netty:5.11.1")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
  getByName("check") {
    dependsOn(":ktlintCheck", "detekt")
  }
}

tasks.named<JavaExec>("bootRun") {
  systemProperty("spring.profiles.active", "dev,localstack,docker")
}
repositories {
  mavenCentral()
}

detekt {
  config = files("src/test/resources/detekt-config.yml")
  buildUponDefaultConfig = true
  ignoreFailures = true
}
