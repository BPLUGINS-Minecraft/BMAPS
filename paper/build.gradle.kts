plugins {
    id("java")
    id("com.gradleup.shadow")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")

    implementation(project(":core"))
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("BMAPS")

        // Verhindert Klassen-Konflikte mit anderen Plugins, die evtl. auch
        // sqlite-jdbc/HikariCP einbinden.
        relocate("org.sqlite", "b.bplugins.plugin.maps.libs.sqlite")
        relocate("com.zaxxer.hikari", "b.bplugins.plugin.maps.libs.hikari")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("paper-plugin.yml") {
            expand("version" to project.version)
        }
    }
}