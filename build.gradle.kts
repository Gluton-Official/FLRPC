import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.builtins.StandardNames.FqNames.target
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
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
val flStudioVersion = extra["flStudio.version"] as String

repositories {
    mavenCentral()
}

val cInteropsDir: File = project.file("src/nativeInterop/cinterop/")
val libsDir: File = project.file("src/nativeInterop/libs/")

val dynLibs = listOf(libsDir.resolve("discord_game_sdk/discord_game_sdk.dll"))

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
//    linuxX64("linux") // Konan glibc 2.19 is too old for discord sdk
//    linuxArm64() // not supported by okio (https://github.com/square/okio/issues/1171), nor discord game sdk?
//    macosX64() // Konan glibc 2.19 is also too old maybe?
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

kotlin.targets.withType<KotlinNativeTarget>()
    .flatMap { NativeBuildType.values().map(it.binaries::getExecutable) }
    .forEach { executable -> with(executable) {
        val resources = run {
            fun KotlinSourceSet.getAllParents(): List<KotlinSourceSet> =
                listOf(this) + dependsOn.flatMap { it.getAllParents() }.toMutableList()
            kotlin.sourceSets["${target.name}Main"]!!.getAllParents().flatMap { it.resources }
        }

        runTask?.doFirst {
            // include dynamic libraries and resources in runtime directory
            copy {
                from(dynLibs, resources)
                into(outputDirectory)
            }
        }

        val buildTypeName = buildType.name.lowercase()
        val capitalizedBuildTypeName = buildTypeName.uppercaseFirstChar()
        val capitalizedTargetName = target.name.uppercaseFirstChar()
        val packageTask = tasks.register<Zip>("package$capitalizedBuildTypeName$capitalizedTargetName") {
            group = "package"
            description = "Packages Kotlin/Native executable with libs for target '${target.name}'"

            if (buildType != NativeBuildType.RELEASE) {
                archiveAppendix.set(buildTypeName)
            }
            archiveVersion.set("${project.version}_$flStudioVersion")
            archiveClassifier.set(target.konanTarget.name)

            from(outputDirectory.resolve("FLRPC.${target.konanTarget.family.exeSuffix}"), dynLibs, resources)
            into(project.name)
            destinationDirectory.set(buildDir.resolve("packages"))

            dependsOn(linkTask)
        }

        if (target.konanTarget == HostManager.host) {
            if (runTask != null) {
                tasks.register("run${capitalizedBuildTypeName}ExecutableCurrentOS") {
                    group = "run"
                    description = "Executes Kotlin/Native executable ${buildTypeName}Executable for current platform target ('${target.name}')"
                    dependsOn(runTask)
                }
            }

            tasks.register("package${capitalizedBuildTypeName}CurrentOS") {
                group = "package"
                description = "Packages Kotlin/Native executable ${buildTypeName}Executable with libs for for current platform target ('${target.name}')"
                dependsOn(packageTask)
            }
        }
    }}