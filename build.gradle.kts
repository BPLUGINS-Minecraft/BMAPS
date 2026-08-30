plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5" apply false
}

allprojects {
    group = "b.bplugins.plugin.maps"
    version = "2.0.0-ALPHA"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.opencollab.dev/main/") // Floodgate-API
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}