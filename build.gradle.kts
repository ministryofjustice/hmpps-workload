plugins {
  id("com.gorylenko.gradle-git-properties") version "2.5.2"
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.0.0"
  kotlin("plugin.spring") version "2.2.10"
  kotlin("plugin.jpa") version "2.2.10"
  kotlin("jvm") version "2.2.10"
  id("io.gitlab.arturbosch.detekt").version("1.23.8")
  id("org.owasp.dependencycheck") version "12.1.3"
  kotlin("plugin.allopen").version("2.2.10")
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
  nvd.datafeedUrl = "file:///opt/vulnz/cache"
}

allOpen {
  annotations("javax.persistence.Entity")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.10")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.12")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux")
  testImplementation("org.springframework.boot:spring-boot-starter-web")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2")

  // go to open telemetry, when upgrading to spring boot 3 these can be removed
  implementation("io.opentelemetry:opentelemetry-api:1.53.0")
  implementation("com.microsoft.azure:applicationinsights-core:3.7.4")
  agentDeps("com.microsoft.azure:applicationinsights-agent:3.7.4")

  implementation("uk.gov.service.notify:notifications-java-client:5.2.1-RELEASE")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.3")

  runtimeOnly("com.zaxxer:HikariCP")
  implementation("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.7")

  testImplementation("io.jsonwebtoken:jjwt-impl:0.13.0")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.13.0")
  testImplementation("org.mock-server:mockserver-netty:5.15.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.mockk:mockk:1.14.5")
  testImplementation("com.ninja-squad:springmockk:4.0.2")

  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
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

kotlin {
  compilerOptions {
    freeCompilerArgs.add("-Xannotation-default-target=param-property")
  }
}

tasks.named<JavaExec>("bootRun") {
  systemProperty("spring.profiles.active", "dev,docker")
}

detekt {
  toolVersion = "1.23.8"
  config.setFrom("src/test/resources/detekt-config.yml")
  buildUponDefaultConfig = true
}
// fix to prevent the mismatch of kotlin versions for detekt
configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion("2.0.21")
    }
  }
}

ktlint {
  version.set("1.5.0")
}
