pluginManagement {
    plugins {
        kotlin("jvm") version "2.2.20"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "BMAPS"

include("core")
include("paper")

// Noch leer / noch keine build.gradle.kts vorhanden -
// erst einkommentieren, sobald der jeweilige Modul-Ordner tatsächlich
// Inhalt hat, sonst bricht der komplette Gradle-Sync ab (auch für
// core/paper), weil Gradle für JEDES include() ein gültiges Modul erwartet.
// include("geyser")
// include("velocity")
// include("bungeecord")
// include("fabric")
// include("forge")
// include("neoforge")
// include("quilt")