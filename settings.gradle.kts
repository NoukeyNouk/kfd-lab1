pluginManagement { repositories { gradlePluginPortal(); mavenCentral() }
    plugins {
        kotlin("jvm") version "2.4.21"
    }
}

dependencyResolutionManagement { repositories { mavenCentral() } }
rootProject.name = "schedule-lab"
include("app")
