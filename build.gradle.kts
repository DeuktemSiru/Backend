plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
	jacoco
}

group = "com.deuktemsiru"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
	implementation("tools.jackson.module:jackson-module-kotlin")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:postgresql:1.20.4")
	testImplementation("org.testcontainers:junit-jupiter:1.20.4")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
		allWarningsAsErrors = true
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		events("failed", "skipped")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required = true
		html.required = true
	}
}

val verifyNoBackendStubs by tasks.registering {
	group = "verification"
	description = "Fails the build when unfinished backend stubs are committed."

	val sourceTree = fileTree("src/main/kotlin") {
		include("**/*.kt")
		exclude("**/dto/BuyerStoreDto.kt")
	}
	val forbidden = listOf(
		"UnsupportedOperationException",
		"NotImplementedError",
		"TODO(",
		"TODO:",
		"미구현",
	)

	inputs.files(sourceTree)

	doLast {
		val violations = sourceTree.files.flatMap { file ->
			file.readLines().mapIndexedNotNull { index, line ->
				val token = forbidden.firstOrNull { it in line }
				if (token == null) null else "${file.relativeTo(projectDir)}:${index + 1}: $token"
			}
		}

		if (violations.isNotEmpty()) {
			throw GradleException(
				"Unfinished backend stubs found:\n" + violations.joinToString("\n"),
			)
		}
	}
}

tasks.check {
	dependsOn(verifyNoBackendStubs)
	dependsOn(tasks.jacocoTestReport)
}
