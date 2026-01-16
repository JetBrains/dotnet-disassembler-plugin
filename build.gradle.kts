import com.jetbrains.plugin.structure.base.utils.isFile
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants
import kotlin.io.path.absolute
import kotlin.io.path.isDirectory

plugins {
    id("java")
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.changelog)
    id("org.jetbrains.intellij.platform") version "2.10.4"     // See https://github.com/JetBrains/intellij-platform-gradle-plugin/releases
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
    kotlinOptions { jvmTarget = "21" }
}

tasks.compileTestKotlin {
    kotlinOptions { jvmTarget = "21" }
}

tasks.test {
    useTestNG()
}

val dotNetSrcDir = File(projectDir, "src/dotnet")

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

tasks.buildPlugin {
    dependsOn(compileDotNet)
    doLast {
        copy {
            from("${buildDir}/distributions/${rootProject.name}-${version}.zip")
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
            recommended()
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

    val outputFolder = "${rootDir}/src/dotnet/${DotnetPluginId}/bin/${DotnetPluginId}.Rider/${BuildConfiguration}"
    val dllFiles = listOf(
            "$outputFolder/${DotnetPluginId}.dll",
            "$outputFolder/${DotnetPluginId}.pdb",
            "$outputFolder/Microsoft.Extensions.Caching.Abstractions.dll",
            "$outputFolder/Microsoft.Extensions.Caching.Memory.dll",
    )

    dllFiles.forEach({ f ->
        val file = file(f)
        from(file, { into("${rootProject.name}/dotnet") })
    })

    doLast {
        dllFiles.forEach({ f ->
            val file = file(f)
            if (!file.exists()) throw RuntimeException("File ${file} does not exist")
        })
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
