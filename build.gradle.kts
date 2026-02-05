import com.jetbrains.plugin.structure.base.utils.isFile
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory

plugins {
    id("java")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.changelog)
    id("org.jetbrains.intellij.platform") version "2.11.0"     // See https://github.com/JetBrains/intellij-platform-gradle-plugin/releases
    id("me.filippov.gradle.jvm.wrapper") version "0.14.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val DotnetSolution: String by project
val BuildConfiguration: String by project
val ProductVersion: String by project
val DotnetPluginId: String by project
val RiderPluginId: String by project
val PublishToken: String by project



allprojects {
    repositories {
        maven { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    }
}

repositories {
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

tasks.wrapper {
    gradleVersion = "8.8"
    distributionType = Wrapper.DistributionType.ALL
    distributionUrl = "https://cache-redirector.jetbrains.com/services.gradle.org/distributions/gradle-${gradleVersion}-all.zip"
}

version = extra["PluginVersion"] as String


// ============ Plugin File Definitions ==============

val pluginStagingDir = layout.buildDirectory.dir("plugin-staging").get().asFile
val pluginContentDir = file("${pluginStagingDir}/${rootProject.name}")
val signingManifestFile = file("${pluginStagingDir}/files-to-sign.txt")

val dotNetSrcDir = File(projectDir, "src/dotnet")
val dotNetOutputDir = "${dotNetSrcDir}/${DotnetPluginId}/bin/${DotnetPluginId}.Rider/${BuildConfiguration}"

// All .NET files to include in the plugin
val dotNetOutputFiles = listOf(
    "${DotnetPluginId}.dll",
    "${DotnetPluginId}.pdb",
    "Microsoft.Extensions.Caching.Abstractions.dll",
    "Microsoft.Extensions.Caching.Memory.dll",
)

// .NET files that need signing (only our own code, not third-party dependencies)
val dotNetFilesToSign = listOf(
    "${DotnetPluginId}.dll",
)

// JAR files that need signing (only our own code, not third-party dependencies)
val jarFilesToSign = listOf(
    "${rootProject.name}-${version}.jar",
    "${rootProject.name}-${version}-searchableOptions.jar",
)


tasks.processResources {
    from("dependencies.json") { into("META-INF") }
}

tasks.processTestResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

sourceSets {
    main {
        java.srcDir("src/rider/main/java")
        kotlin.srcDir("src/rider/main/kotlin")
        resources.srcDir("src/rider/main/resources")
    }
    test {
        kotlin.srcDir("src/test/kotlin")
        resources.srcDir("src/test/resources")
    }
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.compileTestKotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.test {
    useTestNG()
}


val riderSdkPath by lazy {
    val path = intellijPlatform.platformPath.resolve("lib/DotNetSdkForRdPlugins").absolute()
    if (!path.isDirectory()) error("$path does not exist or not a directory")

    println("Rider SDK path: $path")
    return@lazy path
}

val generateDotNetSdkProperties by tasks.registering {
    val dotNetSdkGeneratedPropsFile = file("build/DotNetSdkPath.Generated.props")
    doLast {
        dotNetSdkGeneratedPropsFile.writeTextIfChanged("""<Project>
  <PropertyGroup>
    <DotNetSdkPath>$riderSdkPath</DotNetSdkPath>
  </PropertyGroup>
</Project>
""")
    }
}

val generateNuGetConfig by tasks.registering {
    val nuGetConfigFile = dotNetSrcDir.resolve("nuget.config")
    doLast {
        nuGetConfigFile.writeTextIfChanged("""<?xml version="1.0" encoding="utf-8"?>
<configuration>
    <packageSources>
        <add key="rider-sdk" value="$riderSdkPath" />
        <add key="nuget.org" value="https://api.nuget.org/v3/index.json" />
    </packageSources>
</configuration>
""")
    }
}

val compileDotNet by tasks.registering {
    dependsOn(generateDotNetSdkProperties, generateNuGetConfig)
    doLast {
        exec {
            executable(layout.projectDirectory.file("dotnet.cmd"))
            args("build", "${DotnetSolution}", "--configuration", BuildConfiguration)
            workingDir(rootDir)
        }
    }
}

val testDotNet by tasks.registering {
    dependsOn(generateDotNetSdkProperties, generateNuGetConfig)
    doLast {
        exec {
            executable(layout.projectDirectory.file("dotnet.cmd"))
            args("test", "${DotnetSolution}", "--logger", "GitHubActions", "--logger", "trx")
            workingDir(rootDir)
        }
    }
}

// ========= Two-Phase Build for Signing Support ================

// Preparation for signing. Build all dll's and jar's and puts them into ${pluginStagingDir}
val preparePluginForSigning by tasks.registering {
    description = "Prepares plugin files for signing and generates signing manifest"
    group = "build"

    // Mirror BuildPluginTask dependencies:
    // - prepareSandbox provides the main plugin directory
    // - jarSearchableOptions provides the searchable options JAR
    dependsOn(tasks.prepareSandbox)
    dependsOn(tasks.jarSearchableOptions)

    // Clean staging directory FIRST to prevent poisoning
    doFirst {
        delete(pluginStagingDir)
        mkdir(pluginStagingDir)
    }

    doLast {
        // Step 1: Copy from prepareSandbox.pluginDirectory (same as BuildPluginTask)
        val sandboxPluginDir = tasks.prepareSandbox.get().pluginDirectory.get().asFile
        copy {
            from(sandboxPluginDir)
            into(pluginContentDir)
        }

        // Step 2: Copy searchable options JAR to lib/ (same as BuildPluginTask)
        copy {
            from(tasks.jarSearchableOptions.get().archiveFile)
            into(file("${pluginContentDir}/lib"))
        }

        // Step 3: Generate signing manifest
        val filesToSign = mutableListOf<String>()

        // Add JAR files that need signing (only our own code)
        jarFilesToSign.forEach { jarName ->
            filesToSign.add("${rootProject.name}/lib/${jarName}")
        }

        // Add .NET files that need signing
        dotNetFilesToSign.forEach { fileName ->
            filesToSign.add("${rootProject.name}/dotnet/${fileName}")
        }

        // Write manifest
        signingManifestFile.writeText(filesToSign.joinToString("\n"))

        // Summary
        println("Plugin prepared for signing: ${pluginStagingDir}")
        println("Signing manifest: ${signingManifestFile}")
        println("Files to sign: ${filesToSign.size}")
        filesToSign.forEach { println("  - $it") }
    }
}

// Validates that ${pluginStagingDir} has all required files to assemble the plugin
val validatePluginStaging by tasks.registering {
    description = "Validates that plugin staging directory exists and contains required files"
    group = "build"

    doLast {
        if (!pluginContentDir.exists()) {
            throw RuntimeException(
                "Plugin staging directory not found: ${pluginContentDir}\n" +
                "Run './gradlew preparePluginForSigning' first."
            )
        }

        // Validate expected .NET output files exist
        dotNetOutputFiles.forEach { fileName ->
            val file = file("${pluginContentDir}/dotnet/${fileName}")
            if (!file.exists()) throw RuntimeException("Expected .NET file not found: ${file}")
        }

        // Validate expected JAR files exist
        jarFilesToSign.forEach { jarName ->
            val file = file("${pluginContentDir}/lib/${jarName}")
            if (!file.exists()) throw RuntimeException("Expected JAR file not found: ${file}")
        }
    }
}

// Assembles the final zip-archive by taking the files from ${pluginStagingDir}
val assemblePlugin by tasks.registering(Zip::class) {
    description = "Assembles the final plugin ZIP from staged files"
    group = "build"

    dependsOn(validatePluginStaging)

    from(pluginStagingDir) {
        // Include plugin content directory
        include("${rootProject.name}/**")
        // Exclude signing manifest from final ZIP
        exclude("files-to-sign.txt")
    }

    // Use same naming convention as original BuildPluginTask:
    // archiveBaseName comes from plugin.xml (via IntelliJ plugin extension)
    archiveBaseName.convention(intellijPlatform.projectName)
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    // Register artifact to INTELLIJ_PLATFORM_DISTRIBUTION configuration (same as original BuildPluginTask)
    // This ensures publishPlugin and other dependent tasks can find the archive
    val intellijPlatformDistributionConfiguration = configurations[Constants.Configurations.INTELLIJ_PLATFORM_DISTRIBUTION]
    artifacts.add(intellijPlatformDistributionConfiguration.name, this)

    doLast {
        // Copy to output directory
        copy {
            from(archiveFile)
            into("${rootDir}/output")
        }

        // TODO: See also org.jetbrains.changelog: https://github.com/JetBrains/gradle-changelog-plugin
        val changelogText = file("${rootDir}/CHANGELOG.md").readText()
        val changelogMatches = Regex("(?s)(-.+?)(?=##|$)").findAll(changelogText)
        val changeNotes = changelogMatches.map {
            it.groups[1]!!.value.replace("(?s)- ".toRegex(), "\u2022 ").replace("`", "").replace(",", "%2C").replace(";", "%3B")
        }.take(1).joinToString()

        exec {
            executable(layout.projectDirectory.file("dotnet.cmd"))
            args("pack", "${DotnetSolution}", "--configuration", BuildConfiguration, "--output", "${rootDir}/output", "/p:PackageReleaseNotes=${changeNotes}", "/p:PackageVersion=${version}")
            workingDir(rootDir)
        }

        println("Plugin assembled: output/${rootProject.name}-${version}.zip")
    }
}

tasks.buildPlugin {
    // Disable the IntelliJ plugin's default buildPlugin behavior for ZIP creation
    // and make it use our two-phase approach instead
    actions.clear()

    // Make it depend on our assembly task
    dependsOn(preparePluginForSigning)
    dependsOn(assemblePlugin)

    // Phase 2 (assemblePlugin + all its dependencies) must run after Phase 1
    assemblePlugin.get().mustRunAfter(preparePluginForSigning)
    assemblePlugin.get().taskDependencies.getDependencies(assemblePlugin.get()).forEach { task ->
        task.mustRunAfter(preparePluginForSigning)
    }
}

dependencies {
    intellijPlatform {
        rider(ProductVersion, useInstaller = false)
        jetbrainsRuntime()
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Bundled)

        // TODO: add plugins
        // bundledPlugin("uml")
        // bundledPlugin("com.jetbrains.ChooseRuntime:1.0.9")
    }

    testImplementation("org.testng:testng:7.10.2")
}

intellijPlatform {
    pluginVerification {
        ides {
            create(IntelliJPlatformType.Rider, ProductVersion)
        }
    }

    pluginConfiguration {
        version = providers.gradleProperty("PluginVersion")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
    }
}

changelog {
    groups.empty()
    repositoryUrl.set(providers.gradleProperty("PluginRepositoryUrl"))
}

tasks.runIde {
    // Match Rider's default heap size of 1.5Gb (default for runIde is 512Mb)
    maxHeapSize = "1500m"
}

tasks.patchPluginXml {
    // TODO: See also org.jetbrains.changelog: https://github.com/JetBrains/gradle-changelog-plugin
    val changelogText = file("${rootDir}/CHANGELOG.md").readText()
    val changelogMatches = Regex("(?s)(-.+?)(?=##|\$)").findAll(changelogText)

    changeNotes.set(changelogMatches.map {
        it.groups[1]!!.value.replace("(?s)\r?\n".toRegex(), "<br />\n")
    }.take(1).joinToString())

    // Extract multiple sections from README.md using markers
    val readmeText = file("${rootDir}/README.md").readText()
    val sectionRegex = Regex("(?s)<!-- Plugin description(?:: (\\w+))? -->\\s*(.+?)\\s*<!-- Plugin description end(?:: \\w+)? -->")
    val sections = sectionRegex.findAll(readmeText).map { match ->
        val sectionName = match.groups[1]?.value ?: "main"
        val content = match.groups[2]?.value ?: ""
        sectionName to markdownToHTML(content)
    }.toList()
    
    if (sections.isNotEmpty()) {
        val readmeDescription = sections.joinToString("\n") { it.second }

        val pluginXmlFile = file("${rootDir}/src/rider/main/resources/META-INF/plugin.xml")
        val pluginXmlText = pluginXmlFile.readText()
        val descriptionRegex = Regex("(?s)<description><!\\[CDATA\\[(.+?)]]></description>")
        val existingDescription = descriptionRegex.find(pluginXmlText)?.groups?.get(1)?.value?.trim() ?: ""

        val combinedDescription = if (existingDescription.isNotEmpty()) {
            "$readmeDescription\n$existingDescription"
        } else {
            readmeDescription
        }
        
        pluginDescription.set(combinedDescription)
    }
}

tasks.prepareSandbox {
    dependsOn(compileDotNet)

    // Use shared .NET output files list
    dotNetOutputFiles.forEach { fileName ->
        from("${dotNetOutputDir}/${fileName}") {
            into("${rootProject.name}/dotnet")
        }
    }

    doLast {
        // Validation: ensure all .NET output files exist
        dotNetOutputFiles.forEach { fileName ->
            val file = file("${dotNetOutputDir}/${fileName}")
            if (!file.exists()) throw RuntimeException("File ${file} does not exist")
        }
    }
}

tasks.publishPlugin {
    dependsOn(testDotNet)
    dependsOn(tasks.buildPlugin)
    token.set(providers.environmentVariable("PUBLISH_TOKEN"))

    doLast {
        exec {
            executable(layout.projectDirectory.file("dotnet.cmd"))
            val nugetToken = System.getenv("PUBLISH_TOKEN") ?: ""
            args("nuget", "push", "output/${DotnetPluginId}.${version}.nupkg", "--api-key", nugetToken, "--source", "https://plugins.jetbrains.com")
            workingDir(rootDir)
        }
    }
}

fun File.writeTextIfChanged(content: String) {
    val bytes = content.toByteArray()

    if (!exists() || !readBytes().contentEquals(bytes)) {
        println("Writing $path")
        parentFile.mkdirs()
        writeBytes(bytes)
    }
}

val riderModel: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

artifacts {
    add(riderModel.name, provider {
        intellijPlatform.platformPath.resolve("lib/rd/rider-model.jar").also {
            check(it.isFile) {
                "rider-model.jar is not found at $riderModel"
            }
        }
    }) {
        builtBy(Constants.Tasks.INITIALIZE_INTELLIJ_PLATFORM_PLUGIN)
    }
}
