repositories {
  maven { url = uri("https://repo.rd.zedev.net/artifactory/public/") }
  maven { url = uri("https://repo.rd.zedev.net/artifactory/jooq-pro/") }
}

plugins {
  kotlin("jvm") version "1.7.20"
}

object Versions {
  const val jooq = "3.17.4"
  const val kotest = "5.5.3"
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

dependencies {
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
  testImplementation("org.jooq.pro:jooq:${Versions.jooq}")
  testImplementation("org.jooq.pro:jooq-kotlin:${Versions.jooq}")
  testImplementation("org.jooq.pro:jooq-kotlin-coroutines:${Versions.jooq}")
  testImplementation("io.kotest:kotest-runner-junit5:${Versions.kotest}")
  testImplementation("io.kotest:kotest-assertions-core:${Versions.kotest}")
  testImplementation("com.oracle.database.r2dbc:oracle-r2dbc:1.0.0")
  testImplementation("org.testcontainers:oracle-xe:1.17.5")

  testRuntimeOnly("ch.qos.logback:logback-classic:1.4.4")
}
