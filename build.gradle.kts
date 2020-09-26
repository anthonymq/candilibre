plugins {
    kotlin("multiplatform") version("1.4.0")
    id("org.jetbrains.kotlin.plugin.serialization")version("1.4.0")
}

repositories {
    jcenter()
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlinx/")
}

val os = org.gradle.internal.os.OperatingSystem.current()!!

kotlin {
    when {
        os.isWindows -> mingwX64()
        os.isMacOsX -> macosX64()
        //os.isLinux -> linuxX64()
        else -> throw Error("Unknown host")
    }.binaries.executable {
        if (os.isWindows) {
            windowsResources("candilibre.rc")
            linkerOpts("-mwindows")
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9-native-mt")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.0.0-RC")
                implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.4.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val mingwX64Main by getting {
            dependencies {
                implementation("com.github.msink:libui:0.1.8")
            }
        }
    }
}

// Credit goes to https://github.com/msink/kotlin-libui for this method
fun org.jetbrains.kotlin.gradle.plugin.mpp.Executable.windowsResources(rcFileName: String) {
    val taskName = linkTaskName.replaceFirst("link", "windres")
    val inFile = compilation.defaultSourceSet.resources.sourceDirectories.singleFile.resolve(rcFileName)
    val outFile = buildDir.resolve("processedResources/$taskName.res")

    val windresTask = tasks.create<Exec>(taskName) {
        val konanUserDir = System.getenv("KONAN_DATA_DIR") ?: "${System.getProperty("user.home")}/.konan"
        val konanLlvmDir = "$konanUserDir/dependencies/msys2-mingw-w64-x86_64-clang-llvm-lld-compiler_rt-8.0.1/bin"

        inputs.file(inFile)
        outputs.file(outFile)
        commandLine("$konanLlvmDir/windres", inFile, "-D_${buildType.name}", "-O", "coff", "-o", outFile)
        environment("PATH", "$konanLlvmDir;${System.getenv("PATH")}")

        dependsOn(compilation.compileKotlinTask)
    }

    linkTask.dependsOn(windresTask)
    linkerOpts(outFile.toString())
}