pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.xpdustry.com/releases") {
            name = "xpdustry-releases"
            mavenContent { releasesOnly() }
        }
    }
}

rootProject.name = "flex"
