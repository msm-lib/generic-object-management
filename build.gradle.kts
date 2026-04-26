plugins {
    java
    id("java-library")
    id("maven-publish")
//    id("org.springframework.boot") version "3.4.8"
//    id("io.spring.dependency-management") version "1.1.7"

}

group = "com.msm"
version = "1.0-SNAPSHOT"


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
val lombokVersion = "1.18.30"
val jwtVersion = "0.12.6"
val javaJwtVersion = "4.4.0"
val springSecurityData = "5.8.0"
val hikariCPVersion = "5.0.1"
val openFeignVersion = "4.2.1"
val awssdkVersion = "2.29.47"
val guavaVersion = "33.5.0-jre"
val kogitoVersion = "2.44.0.Alpha"
val droolsVersion = "10.1.0"
val jooqVersion = "3.21.1"
//https://jitpack.io/#msm-lib/commons/3.1
val msmCommonVersion = "1.4.8"

dependencies {

    implementation("com.google.guava:guava:${guavaVersion}")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.postgresql:postgresql")
    implementation("com.zaxxer:HikariCP:${hikariCPVersion}")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-data:${springSecurityData}")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // OpenFeign
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:$openFeignVersion")

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")


    implementation("io.jsonwebtoken:jjwt:${jwtVersion}")
    implementation("com.auth0:java-jwt:${javaJwtVersion}")

    //AWS S3
    implementation("software.amazon.awssdk:s3:$awssdkVersion")
    implementation("software.amazon.awssdk:sts:$awssdkVersion")
    implementation("software.amazon.awssdk:sqs:${awssdkVersion}")

    //Kie drools
    implementation("org.kie.kogito:kogito-api:${kogitoVersion}")
    implementation("org.kie.kogito:kogito-drools:${kogitoVersion}")
    implementation("org.drools:drools-core:${droolsVersion}")
    implementation("org.drools:drools-compiler:${droolsVersion}")
    implementation("org.drools:drools-wiring-static:${droolsVersion}")
    implementation("org.drools:drools-mvel:${droolsVersion}")



//    implementation("org.apache.commons:commons-lang3:3.18.0")

    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    //logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // For Gradle (build.gradle)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")
    // Use the latest compatible version
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    implementation("org.apache.commons:commons-collections4:4.5.0")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")


    //For jackson
    implementation("com.fasterxml.jackson.core:jackson-core:2.20.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.20")

    //excel, csv
    implementation("org.apache.poi:poi:5.5.0")
    implementation("org.apache.poi:poi-ooxml:5.5.0")
    implementation("org.apache.commons:commons-csv:1.14.1")
    // easy rules
    implementation("org.jeasy:easy-rules-core:4.1.0")
    implementation("org.jeasy:easy-rules-mvel:4.1.0")

    //For local test
//    implementation(files("/Users/danhnh/Infomation/Development/Lib/common/build/libs/commons-1.0.0.jar"))
    implementation("com.github.msm-lib:commons:${msmCommonVersion}") {
        exclude(group = "org.slf4j")
    }

    // QueryDSL JPA
    implementation("com.querydsl:querydsl-core:5.1.0")
    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jakarta")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    //spring context indexer
    annotationProcessor("org.springframework:spring-context-indexer")

    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.jooq:jooq:3.21.1")

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