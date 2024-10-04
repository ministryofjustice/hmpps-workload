plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.3"
  kotlin("plugin.spring") version "2.0.0"
  kotlin("plugin.jpa") version "2.0.0"
  id("io.gitlab.arturbosch.detekt").version("1.23.6")
  kotlin("plugin.allopen").version("2.0.0")
}

configurations {
  implementation { exclude(module = "spring-boot-starter-web") }
  implementation { exclude(module = "spring-boot-starter-tomcat") }
  implementation { exclude(module = "applicationinsights-spring-boot-starter") }
  implementation { exclude(module = "applicationinsights-logging-logback") }
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencyCheck {
  suppressionFiles.add("suppressions.xml")
}

allOpen {
  annotations("javax.persistence.Entity")
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:4.0.1")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.6.0")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux")
  testImplementation("org.springframework.boot:spring-boot-starter-web")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.1")

  // go to open telemetry, when upgrading to spring boot 3 these can be removed
  implementation("io.opentelemetry:opentelemetry-api:1.40.0")
  implementation("com.microsoft.azure:applicationinsights-core:3.5.3")
  agentDeps("com.microsoft.azure:applicationinsights-agent:3.5.3")

  implementation("uk.gov.service.notify:notifications-java-client:5.1.0-RELEASE")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.8.1")

  runtimeOnly("com.zaxxer:HikariCP")
  implementation("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.3")

  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("org.mock-server:mockserver-netty:5.15.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.1")
  testImplementation("io.mockk:mockk:1.13.12")
  testImplementation("com.ninja-squad:springmockk:4.0.2")

  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
repositories {
  mavenCentral()
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
  }

  compileKotlin {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
  }

  compileTestKotlin {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
  }

  getByName("check") {
    dependsOn(":ktlintCheck", "detekt")
  }
}

tasks.named<JavaExec>("bootRun") {
  systemProperty("spring.profiles.active", "dev,docker")
}

detekt {
  config.setFrom("src/test/resources/detekt-config.yml")
  buildUponDefaultConfig = true
}
// fix to prevent the mismatch of kotlin versions for detekt
configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion("1.9.23")
    }
  }
}
