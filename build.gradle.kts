plugins {
    java
    id("java-library")
    id("maven-publish")
//    id("org.springframework.boot") version "3.4.8"
    id("io.spring.dependency-management") version "1.1.7"

}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.8")
    }
}

group = "com.msm"
version = "1.0"


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral{
        metadataSources {
            mavenPom()
            artifact()
        }
        maven { url = uri("https://jitpack.io") }
    }
    mavenCentral()
}
extra["jooq.version"] = "3.21.1"
//https://jitpack.io/#msm-lib/commons/3.1
val msmCommonVersion = "1.5.4"

dependencies {

    // Spring core
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.0"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    // Spring auto config
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    // nếu có REST API trong lib
    implementation("org.springframework:spring-web")

    // spring security
    implementation("org.springframework.security:spring-security-core")

    // validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // logging helper
    implementation("org.slf4j:slf4j-api")

    // optional: Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    //spring doc
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

//    implementation(files("/Users/danhnh/Infomation/Development/Lib/common/build/libs/commons-1.0.0.jar"))
    implementation("com.github.msm-lib:commons:${msmCommonVersion}") { exclude(group = "org.slf4j") }

    implementation("com.querydsl:querydsl-core:5.1.0")
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jakarta")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // easy rules
    implementation("org.jeasy:easy-rules-core:4.1.0")
    implementation("org.jeasy:easy-rules-mvel:4.1.0")

    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.jooq:jooq:3.21.1")
}

tasks.jar {
    enabled = true
}

tasks.withType<Test> {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "generic-object-management"
            version = project.version.toString()
        }
    }
}