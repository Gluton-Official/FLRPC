
plugins {
    kotlin("multiplatform") version "1.9.0"
}

group = "dev.gluton"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
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
                            project.file("discord_game_sdk/c/")
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
        }
    }
    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3-native-mt")
                implementation("com.squareup.okio:okio:3.4.0")
            }
        }
        val nativeTest by getting
    }
}

tasks {
    val includeDllsDebug = register<Copy>("includeDllsDebug") {
        from(project.file("discord_game_sdk/lib/x86_64/discord_game_sdk.dll"))
        into(buildDir.resolve("bin/native/debugExecutable"))
    }

    "linkDebugExecutableNative" {
        dependsOn(includeDllsDebug)
    }
}