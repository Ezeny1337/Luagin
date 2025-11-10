import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.run-paper") version "3.0.0"
}

group = "tech.ezeny"
version = "3.4.0"

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
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    implementation("party.iroiro.luajava:luajava:4.0.2")
    implementation("party.iroiro.luajava:luajit:4.0.2")
    runtimeOnly("party.iroiro.luajava:luajit-platform:4.0.2:natives-desktop")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.insert-koin:koin-core:4.1.1")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("com.mysql:mysql-connector-j:9.4.0")
    implementation("com.github.oshi:oshi-core:6.9.0")
    implementation("io.ktor:ktor-server-core-jvm:3.3.0")
    implementation("io.ktor:ktor-server-netty-jvm:3.3.0")
    implementation("io.ktor:ktor-server-auth-jvm:3.3.0")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:3.3.0")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.3.0")
    implementation("io.ktor:ktor-serialization-jackson-jvm:3.3.0")
    implementation("io.ktor:ktor-server-cors-jvm:3.3.0")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

// 设置编译编码
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
        freeCompilerArgs.set(listOf("-Xjsr305=strict", "-Xjvm-default=all"))
    }
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
        // 设置运行时JVM参数，确保中文正常显示
        jvmArgs("-Dfile.encoding=UTF-8", "-Dconsole.encoding=UTF-8", "-Dsun.stdout.encoding=UTF-8", "-Dsun.stderr.encoding=UTF-8")
    }
}
