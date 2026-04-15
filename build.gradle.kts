import java.io.BufferedReader

val patch = "INDEV"

val commitHash = Runtime
    .getRuntime()
    .exec(arrayOf("git", "rev-parse", "--short", "HEAD"))
    .let { process ->
        process.waitFor()
        val output = process.inputStream.use {
            it.bufferedReader().use(BufferedReader::readText)
        }
        process.destroy()
        output.trim()
    }

plugins {
    kotlin("jvm") version "2.4.0-Beta1"
    kotlin("kapt") version "2.4.0-Beta1"
    id("com.gradleup.shadow") version "9.4.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "moe.oof"
version = "$patch-Build-$commitHash"

kotlin {
    jvmToolchain(25)
    compilerOptions {
        javaParameters = true
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://maven.noxcrew.com/public") {
        name = "noxcrewMavenPublic"
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    paperweight.paperDevBundle("26.1.2.build.+")

    implementation("org.incendo:cloud-paper:2.0.0-beta.15")
    implementation("org.incendo:cloud-annotations:2.0.0")
    implementation("org.incendo:cloud-kotlin-extensions:2.0.0")
    implementation("org.incendo:cloud-kotlin-coroutines-annotations:2.0.0")
    kapt("org.incendo:cloud-kotlin-coroutines-annotations:2.0.0")
    implementation("org.incendo:cloud-kotlin-extensions:2.0.0")
    implementation("org.incendo:cloud-processors-confirmation:1.0.0-rc.1")
    implementation("io.ktor:ktor-client-core:3.4.2")
    implementation("io.ktor:ktor-client-cio:3.4.2")
    implementation("io.ktor:ktor-client-logging:3.4.2")
    implementation("org.spongepowered:configurate-yaml:4.2.0")
    implementation("org.spongepowered:configurate-extra-kotlin:4.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("fr.mrmicky:fastboard:2.1.5")

    implementation("com.noxcrew.interfaces:interfaces:2.1.0-SNAPSHOT")
}

tasks {
    compileJava {
        options.release = 25
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    shadowJar {
        val shadowPkg = "moe.oof.csystem.shade"

        relocate("org.incendo", "${shadowPkg}.org.incendo")
        relocate("org.spongepowered", "${shadowPkg}.org.spongepowered")
        relocate("fr.mrmicky", "${shadowPkg}.fr.mrmicky")

        mergeServiceFiles()
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}