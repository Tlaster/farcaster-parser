import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform") version "1.9.23"
    id("org.jetbrains.kotlinx.kover") version "0.7.3"
    id("com.vanniktech.maven.publish") version "0.25.3"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
}

val libName = "farcaster-parser"
val libGroup = "moe.tlaster"
val libVersion = "0.1.0-SNAPSHOT"

group = libGroup
version = libVersion

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }
    js {
        browser()
        nodejs()
    }
    //    @OptIn(ExperimentalWasmDsl::class)
    //    wasm {
    //        browser()
    //        nodejs()
    //        d8()
    //    }
    ios()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    watchos()
    watchosSimulatorArm64()
    tvos()
    tvosSimulatorArm64()
    mingwX64()
    linuxX64()
    linuxArm64()

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01)
    signAllPublications()
    coordinates(
        groupId = libGroup,
        artifactId = libName,
        version = libVersion,
    )
    pom {
        name.set(libName)
        description.set("Farcaster parser")
        url.set("https://github.com/Tlaster/farcaster-parser")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("Tlaster")
                name.set("James Tlaster")
                email.set("tlaster@outlook.com")
            }
        }
        scm {
            url.set("https://github.com/Tlaster/farcaster-parser")
            connection.set("scm:git:git://github.com/Tlaster/farcaster-parser.git")
            developerConnection.set("scm:git:git://github.com/Tlaster/farcaster-parser.git")
        }
    }
}

ktlint {
    version.set("0.50.0")
}
