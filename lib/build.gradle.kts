plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            api(libs.jna)
            api(libs.jna.platform)
            api(libs.atomicfu)
        }
    }
}

mavenPublishing {
    publishToMavenCentral(true)
    signAllPublications()

    coordinates("dev.brahmkshatriya", "betterwindow", "1.1.0")

    pom {
        name = "Better Window"
        description = "A better Compose Window, that converts the title bar into insets and allowing you to draw behind it."
        inceptionYear = "2025"
        url = "https://github.com/brahmkshatriya/betterwindow"
        licenses {
            license {
                name = "AGPL-3.0 license"
                url = "https://github.com/brahmkshatriya/betterwindow/blob/main/LICENSE.txt"
                distribution = "https://github.com/brahmkshatriya/betterwindow/blob/main/LICENSE.txt"
            }
        }
        developers {
            developer {
                id = "brahmkshatriya"
                name = "Shivam"
                url = "https://github.com/brahmkshatriya/"
            }
        }
        scm {
            url = "https://github.com/brahmkshatriya/betterwindow/"
            connection = "scm:git:git://github.com/brahmkshatriya/betterwindow.git"
            developerConnection = "scm:git:ssh://git@github.com/brahmkshatriya/betterwindow.git"
        }
    }
}