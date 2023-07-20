import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName

plugins {
    kotlin("multiplatform") version "1.9.0"
}

group = "dev.gluton"
version = "0.1.0"

repositories {
    mavenCentral()
}

val cInteropsDir: File = project.file("src/nativeInterop/cinterop/")

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
    linuxX64("linux")
//    linuxArm64() // not supported by okio (https://github.com/square/okio/issues/1171), nor discord game sdk?
    macosX64()
    macosArm64()

    targets.withType<KotlinNativeTarget> nativeTarget@{
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.addAll(
                    "-Xcontext-receivers",
                )
            }
            cinterops {
                val discordSdk by creating {
                    defFile(cInteropsDir.resolve("discord_game_sdk.def"))
                    packageName("discord.gamesdk")
                    includeDirs {
                        allHeaders(project.file("libs/discord_game_sdk/c/"))
                    }
                }
            }
        }
        binaries {
            all {
                with(konanTarget) {
                    val arch = architecture.asDiscordGameSDKArch()
                    linkerOpts += when (family) {
                        Family.MINGW -> listOf(
                            "-L$projectDir/libs/discord_game_sdk/lib/$arch",
                            "-ldiscord_game_sdk",
                        )
                        else -> emptyList()
                    } + "-v"
                }

                if (outputKind == NativeOutputKind.EXECUTABLE) {
                    val buildType = buildType.name.lowercase().uppercaseFirstChar()
                    val buildTargetName = this@nativeTarget.name.uppercaseFirstChar()
                    val arch = konanTarget.architecture.asDiscordGameSDKArch()
                    val libFileExtension = konanTarget.family.dynamicSuffix
                    val includeLibs = tasks.register<Copy>("includeLibs$buildType$buildTargetName") {
                        group = "includeLibs"
                        from(project.file("libs/discord_game_sdk/lib/$arch/discord_game_sdk.$libFileExtension"))
                        into(outputDirectory)
                    }
                    linkTask.finalizedBy(includeLibs)
                }
            }
            executable {
                baseName = "${project.name}-${project.version}"
                entryPoint = "dev.gluton.flrpc.main"
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
        val linuxMain by getting
        val linuxTest by getting
        val macosMain by creating { dependsOn(commonMain) }
        val macosTest by creating { dependsOn(commonTest) }
        val macosX64Main by getting { dependsOn(macosMain) }
        val macosX64Test by getting { dependsOn(macosTest) }
        val macosArm64Main by getting { dependsOn(macosMain) }
        val macosArm64Test by getting { dependsOn(macosTest) }
    }
}

// generates runDebugExecutable and runReleaseExecutable tasks for current platform
NativeBuildType.values().forEach {
    val type = "${it.name.lowercase()}Executable"
    val taskName = "run${type.uppercaseFirstChar()}"
    tasks.register(taskName) {
        group = "run"
        description = "Executes Kotlin/Native executable $type for current platform target"
        dependsOn("$taskName${platformTarget.uppercaseFirstChar()}")
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