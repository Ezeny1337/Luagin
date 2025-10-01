plugins {
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "9.0.0-beta12"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "tech.ezeny"
version = "3.2.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io") {
        name = "jitpack"
    }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.9-R0.1-SNAPSHOT")
    implementation("party.iroiro.luajava:luajava:4.0.2")
    implementation("party.iroiro.luajava:luajit:4.0.2")
    runtimeOnly("party.iroiro.luajava:luajit-platform:4.0.2:natives-desktop")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.insert-koin:koin-core:4.0.4")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("com.mysql:mysql-connector-j:9.3.0")
    implementation("com.github.oshi:oshi-core:6.8.2")
    implementation("io.ktor:ktor-server-core-jvm:3.2.2")
    implementation("io.ktor:ktor-server-netty-jvm:3.2.2")
    implementation("io.ktor:ktor-server-auth-jvm:3.2.2")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:3.2.2")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.2.2")
    implementation("io.ktor:ktor-serialization-jackson-jvm:3.2.2")
    implementation("io.ktor:ktor-server-cors-jvm:3.2.2")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

// 自动拷贝前端 build 到 resources
val copyWebpanelDist = tasks.register<Copy>("copyWebpanelDist") {
    val targetDir = file("src/main/resources/webpanel/dist")

    doFirst {
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
    }

    from("webpanel/dist")
    into(targetDir)
}

tasks {
    build {
        dependsOn("shadowJar")
    }

    processResources {
        dependsOn(copyWebpanelDist)
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

        exclude(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
            "META-INF/INDEX.LIST",
            "META-INF/*.kotlin_module",
            "META-INF/io.netty.versions.properties"
        )
    }

    runServer {
        minecraftVersion("1.21")
    }
}
