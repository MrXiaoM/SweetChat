plugins {
    java
    `maven-publish`
    id ("com.gradleup.shadow") version "8.3.0"
    id ("com.github.gmazzo.buildconfig") version "5.6.7"
}

buildscript {
    repositories.mavenCentral()
    dependencies.classpath("top.mrxiaom:LibrariesResolver-Gradle:1.7.6")
}
val base = top.mrxiaom.gradle.LibraryHelper(project)

group = "top.mrxiaom.sweet.chat"
version = "1.0.2"
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
    compileOnly("com.github.retrooper:packetevents-spigot:2.9.5")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.NEZNAMY:TAB-API:4.1.8")

    base.library("net.kyori:adventure-api:4.22.0")
    base.library("net.kyori:adventure-platform-bukkit:4.4.0")
    base.library("net.kyori:adventure-text-minimessage:4.22.0")
    base.library("net.kyori:adventure-text-serializer-plain:4.22.0")
    base.library("com.zaxxer:HikariCP:4.0.3")

    implementation("top.mrxiaom:EvalEx-j8:3.4.0")
    implementation("de.tr7zw:item-nbt-api:2.15.5")
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
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    withSourcesJar()
    withJavadocJar()
}
tasks {
    shadowJar {
        mapOf(
            "top.mrxiaom.pluginbase" to "base",
            "com.ezylang.evalex" to "evalex",
            "de.tr7zw.changeme.nbtapi" to "nbtapi",
            "com.tcoded.folialib" to "folialib",
        ).forEach { (original, target) ->
            relocate(original, "$shadowGroup.$target")
        }
    }
    val copyTask = create<Copy>("copyBuildArtifact") {
        dependsOn(shadowJar)
        from(shadowJar.get().outputs)
        rename { "${project.name}-$version.jar" }
        into(rootProject.file("out"))
    }
    build {
        dependsOn(copyTask)
    }
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
            options.release.set(targetJavaVersion)
        }
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(sourceSets.main.get().resources.srcDirs) {
            expand(mapOf("version" to version))
            include("plugin.yml")
        }
    }
    javadoc {
        (options as? StandardJavadocDocletOptions)?.apply {
            locale("zh_CN")
            charset("UTF-8")
            encoding("UTF-8")
            docEncoding("UTF-8")
            addBooleanOption("keywords", true)
            addBooleanOption("Xdoclint:none", true)

            val currentJavaVersion = JavaVersion.current()
            if (currentJavaVersion > JavaVersion.VERSION_1_9) {
                addBooleanOption("html5", true)
            }
        }
    }
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = rootProject.name
            version = project.version.toString()

            artifact(tasks["shadowJar"]).classifier = null
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }
}
