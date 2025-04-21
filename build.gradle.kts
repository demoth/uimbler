import com.google.protobuf.gradle.id
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.protobuf") version "0.9.4"
}

group = "org.demoth"
version = "1.3.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("io.grpc:grpc-protobuf:1.70.0")
    implementation("io.grpc:grpc-kotlin-stub:1.4.0")
    runtimeOnly("io.grpc:grpc-netty-shaded:1.70.0")
}

compose.desktop {
    application {
        mainClass = "shamble.ShambleKt"

        nativeDistributions {
            targetFormats(TargetFormat.AppImage, TargetFormat.Exe)
            packageName = "compose-app-1"
            packageVersion = project.version.toString()
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.7"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.70.0"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}
