import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.isWindows

plugins {
    kotlin("multiplatform") version "1.9.0"
}

group = "dev.gluton"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = CompilerSystemProperties.OS_NAME.safeValue
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isWindows -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        compilations.getByName("main") {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xcontext-receivers")
            }
            cinterops {
                val discordSdk by creating {
                    defFile(project.file("src/nativeInterop/cinterop/discord_game_sdk.def"))
                    packageName("discord.gamesdk")
                    includeDirs {
                        allHeaders(
                            project.file("libs/discord_game_sdk/c/")
                        )
                    }
                }
                val psapi by creating
                val winver by creating
                val tlhelp32 by creating
            }
        }
        binaries {
            executable {
                entryPoint = "main"
            }
            all {
                if (isWindows && System.getProperty("os.arch") in listOf("amd64", "x86_64")) {
                    linkerOpts += listOf("-L$projectDir/libs/discord_game_sdk/lib/x86_64", "-ldiscord_game_sdk", "-v")
                }
            }
        }
    }
    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")
                implementation("com.squareup.okio:okio:3.4.0")
            }
        }
        val nativeTest by getting
    }
}

tasks {
    val includeDllsDebug = register<Copy>("includeDllsDebug") {
        from(project.file("libs/discord_game_sdk/lib/x86_64/discord_game_sdk.dll"))
        into(buildDir.resolve("bin/native/debugExecutable"))
    }

    "linkDebugExecutableNative" {
        dependsOn(includeDllsDebug)
    }
}