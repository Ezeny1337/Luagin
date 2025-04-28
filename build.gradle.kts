plugins {
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "9.0.0-beta12"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "tech.ezeny"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.luaj:luaj-jse:3.0.1")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    build {
        dependsOn("shadowJar")
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        minimize()
        relocate("org.luaj", "tech.ezeny.luakit.shadow.luaj")
    }

    runServer {
        minecraftVersion("1.21")
    }
}
