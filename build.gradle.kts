plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.plugin.spring)
	alias(libs.plugins.springframework.boot)
	alias(libs.plugins.spring.dependency.management)
}

group = "com.parking"
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
	implementation(libs.bundles.arrow)
	implementation(libs.bundles.spring)
	implementation(libs.kotlin.reflect)
	implementation(libs.hikari)
	runtimeOnly(libs.postgresql)
	implementation(libs.postgresql)
	testImplementation(libs.spring.boot.starter)
	testImplementation(libs.kotlin.test.junit5)
	testRuntimeOnly(libs.junit.platform.launcher)
	testImplementation(libs.mockk)
	testImplementation(libs.spring.boot.starter.test)
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
