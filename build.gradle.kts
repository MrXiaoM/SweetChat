import top.mrxiaom.gradle.LibraryHelper

plugins {
    java
    `maven-publish`
    id ("com.gradleup.shadow") version "9.3.0"
    id ("com.github.gmazzo.buildconfig") version "5.6.7"
}

buildscript {
    repositories.mavenCentral()
    dependencies.classpath("top.mrxiaom:LibrariesResolver-Gradle:1.7.20")
}
val base = LibraryHelper(project)

group = "top.mrxiaom.sweet.chat"
version = "1.0.3"
val targetJavaVersion = 8
val pluginBaseModules = base.modules.run { listOf(library, paper, actions, l10n, misc) }
val shadowGroup = "top.mrxiaom.sweet.chat.libs"

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.helpch.at/releases/")
    maven("https://jitpack.io")
    maven("https://repo.rosewooddev.io/repository/public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:24.0.0")

    compileOnly("com.github.MascusJeoraly:LanguageUtils:1.9")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.NEZNAMY:TAB-API:4.1.8")

    base.library(LibraryHelper.adventure("4.22.0"))
    base.library(base.depend.HikariCP)

    implementation(base.depend.EvalEx)
    implementation(base.depend.nbtapi)
    implementation("com.github.technicallycoded:FoliaLib:0.4.4") { isTransitive = false }
    for (artifact in pluginBaseModules) {
        implementation("$artifact")
    }

    implementation(base.resolver.lite)
}
buildConfig {
    className("BuildConstants")
    packageName("top.mrxiaom.sweet.chat")

    base.doResolveLibraries()
    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("String[]", "RESOLVED_LIBRARIES", base.join())
}

LibraryHelper.initJava(project, base, targetJavaVersion, true)
LibraryHelper.initPublishing(project)

tasks {
    shadowJar {
        configurations.add(project.configurations.runtimeClasspath.get())
        mapOf(
            "top.mrxiaom.pluginbase" to "base",
            "com.ezylang.evalex" to "evalex",
            "de.tr7zw.changeme.nbtapi" to "nbtapi",
            "com.tcoded.folialib" to "folialib",
        ).forEach { (original, target) ->
            relocate(original, "$shadowGroup.$target")
        }
    }
}
