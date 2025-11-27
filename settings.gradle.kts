pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "PosMan"

// ------------------------------------------------------------
// ⚡ RAM Disk Build Cache (Global Cache Location)
// ------------------------------------------------------------
buildCache {
    local {
        directory = file("R:/PosMan/build-cache")
        isEnabled = true
    }
}

include("Logging")
include("Utils")
include("Persistence")
include("Gui")
include("App")
include("Resources")
