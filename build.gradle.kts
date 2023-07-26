import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName

plugins {
    kotlin("multiplatform") version "1.9.0"
}

group = "dev.gluton"
version = extra["flrpc.version"] as String
val flstudioVersion = extra["flstudio.version"] as String

repositories {
    mavenCentral()
}

val cInteropsDir: File = project.file("src/nativeInterop/cinterop/")
val libsDir: File = project.file("src/nativeInterop/libs/")

@Suppress("UNUSED_VARIABLE")
kotlin {
    mingwX64("windows") windows@{
        compilations.all {
            cinterops {
                cInteropsDir.resolve(this@windows.name).apply {
                    val winver by creating { defFile(resolve("winver.def")) }
                    val tlhelp32 by creating { defFile(resolve("tlhelp32.def")) }
                }
            }
        }
    }
//    linuxX64("linux") // Konan glibc 2.19 is too old
//    linuxArm64() // not supported by okio (https://github.com/square/okio/issues/1171), nor discord game sdk?
//    macosX64() // Konan glibc 2.19 is also too old mabye?
//    macosArm64() // Untestable / can't build

    targets.withType<KotlinNativeTarget> nativeTarget@{
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.addAll(
                    "-Xcontext-receivers",
                )
            }

            cinterops {
                val discordGameSdk by creating {
                    val name = "discord_game_sdk"
                    defFile(cInteropsDir.resolve("$name.def"))
                    includeDirs.allHeaders(libsDir.resolve(name))
                }
                val tray by creating {
                    val trayLibDir = libsDir.resolve("tray")
                    includeDirs.allHeaders(trayLibDir)
                    extraOpts("-libraryPath", trayLibDir)
                }
            }
        }
        binaries {
            all {
                linkerOpts += listOf(
                    "-L$libsDir/discord_game_sdk", "-ldiscord_game_sdk",
                )
                if (buildType != NativeBuildType.DEBUG) {
                    linkerOpts += "-mwindows"
                }
            }

            executable {
                entryPoint = "dev.gluton.flrpc.main"

                val buildType = buildType.name.lowercase().uppercaseFirstChar()
                val buildTargetName = this@nativeTarget.name.uppercaseFirstChar()

                val includeLibs = tasks.register<Copy>("includeLibs$buildType$buildTargetName") {
                    group = "includeLibs"

                    from(libsDir.resolve("discord_game_sdk/discord_game_sdk.dll"))
                    into(outputDirectory)

                    dependsOn(linkTask)
                }
                runTask?.dependsOn(includeLibs)

                val includeResources = tasks.register<Copy>("includeResources$buildType$buildTargetName") {
                    group = "includeResources"

                    from(sourceSets.commonMain.get().resources)
                    from(sourceSets.asMap
                        .filterKeys { it.startsWith(buildTargetName, ignoreCase = true) && it.endsWith("main", ignoreCase = true) }
                        .flatMap { it.value.resources }
                    )
                    into(outputDirectory)

                    dependsOn(linkTask)
                }
                runTask?.dependsOn(includeResources)

                tasks.register<Zip>("package$buildType$buildTargetName") {
                    group = "package"
                    description = "Packages Kotlin/Native executable with libs for target ${this@nativeTarget.name}"

                    if (this@executable.buildType != NativeBuildType.RELEASE) {
                        archiveAppendix.set(buildType.lowercase())
                    }
                    archiveVersion.set("${project.version}_$flstudioVersion")
                    archiveClassifier.set(this@nativeTarget.konanTarget.name)

                    from(outputDirectory)
                    include("*")
                    into(project.name)
                    destinationDirectory.set(buildDir.resolve("packages"))

                    dependsOn(includeLibs)
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                languageVersion = KotlinVersion.KOTLIN_2_0.version
                apiVersion = KotlinVersion.KOTLIN_2_0.version
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
                implementation("com.squareup.okio:okio:3.4.0")
                implementation("com.soywiz.korlibs.klogger:klogger:4.0.9")
            }
        }
        val commonTest by getting
        val windowsMain by getting
        val windowsTest by getting
    }
}

// generates runDebugExecutable and runReleaseExecutable tasks for current platform
NativeBuildType.values().forEach {
    val buildType = it.name.lowercase()

    val runExecutableName = "run${buildType.uppercaseFirstChar()}Executable"
    tasks.register("${runExecutableName}CurrentOS") {
        group = "run"
        description = "Executes Kotlin/Native executable ${buildType}Executable for current platform target"
        dependsOn("$runExecutableName${platformTarget.uppercaseFirstChar()}")
    }

    val packageName = "package${buildType.uppercaseFirstChar()}"
    tasks.register("${packageName}CurrentOS") {
        group = "package"
        description = "Packages Kotlin/Native executable ${buildType}Executable with libs for for current platform target"
        dependsOn("$packageName${platformTarget.uppercaseFirstChar()}")
    }
}

val platformTarget: String = with(HostManager.host) {
    when (family) {
        Family.OSX -> presetName
        else -> HostManager.simpleOsName()
    }
}

fun Architecture.asDiscordGameSDKArch(): String = when (this) {
    Architecture.X64 -> "x86_64"
    Architecture.X86 -> "x86"
    Architecture.ARM64 -> "aarch64"
    else -> error("Discord Game SDK doesn't support $this")
}