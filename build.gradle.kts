plugins {
    kotlin("jvm") version "2.2.0-RC3"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "in.mohammadfaizan"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21")
    }

    // Configure shadowJar to relocate Kotlin stdlib to avoid conflicts
    shadowJar {
        // Relocate Kotlin stdlib to avoid conflicts with other plugins
        relocate("kotlin", "${project.group}.${project.name.lowercase()}.kotlin")
        relocate("org.jetbrains", "${project.group}.${project.name.lowercase()}.jetbrains")

        // Set classifier to empty to make this the main jar
        archiveClassifier.set("")

        // Minimize the jar to reduce size
        minimize()
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"

    // Support both plugin.yml and paper-plugin.yml
    filesMatching("plugin.yml") {
        expand(props)
    }
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}