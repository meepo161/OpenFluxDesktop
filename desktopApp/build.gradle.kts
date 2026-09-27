import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.ByteArrayOutputStream
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import javax.inject.Inject

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(17)
}

/** The app version: -PappVersion=1.2.3 (the release workflow passes the tag). */
val appVersion = (findProperty("appVersion") as String?)?.removePrefix("v")?.takeIf { it.isNotBlank() } ?: "2.0.0"

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    // Skia's Windows natives (skiko) come with the host's runtime above only
    // when building on Windows; a Windows package needs them whatever builds it.
    if (findProperty("windowsPackage") == "true") implementation(compose.desktop.windows_x64)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.compose.components.resources)
    testImplementation(libs.kotlin.testJunit)
    // The README demos drive the real screens (DemoRecorder, OPENFLUX_DEMO=<dir>).
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
}

/**
 * The core the app bundles, in resources/<windows|macos|linux> (Compose's
 * appResources layout, where CoreBinary looks for it): openflux-<os>-<arch>
 * [.exe], openflux-core.version and, on Windows, wintun.dll (the full tunnel)
 * and on x64 WinDivert.dll + WinDivert64.sys (the exit's L3). The binaries are
 * not in git. The release workflow builds them before packaging; for a local
 * run or package, prepareCore builds the missing ones for this machine with
 * the local Go, from -PcoreDir=<checkout>, the OpenFlux/ submodule, ../OpenFlux
 * next to this repository, or else a clone of p1neappleXpress/OpenFlux in
 * build/openflux-core. -PskipCore=true runs the app without a core
 * (Settings → Core then takes a file of your own).
 */
abstract class PrepareCore @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Internal abstract val outDir: DirectoryProperty
    @get:Internal abstract val cloneDir: DirectoryProperty
    @get:Internal abstract val sourceDirs: ListProperty<File>
    @get:Internal abstract val goos: Property<String>
    @get:Internal abstract val goarch: Property<String>
    @get:Internal abstract val repository: Property<String>
    @get:Internal abstract val ref: Property<String>

    @get:Internal val coreName: String
        get() = "openflux-${goos.get()}-${goarch.get()}" + if (goos.get() == "windows") ".exe" else ""

    @get:Internal val missing: Boolean
        get() {
            val out = outDir.get().asFile
            return !File(out, coreName).isFile || windowsFiles.any { !File(out, it).isFile }
        }

    @get:Internal val windowsFiles: List<String>
        get() = when {
            goos.get() != "windows" -> emptyList()
            goarch.get() == "amd64" -> listOf("wintun.dll", "WinDivert.dll", "WinDivert64.sys")
            else -> listOf("wintun.dll")
        }

    @TaskAction
    fun prepare() {
        val out = outDir.get().asFile.apply { mkdirs() }
        if (!File(out, coreName).isFile) buildCore(out)
        if ("wintun.dll" in windowsFiles && !File(out, "wintun.dll").isFile) {
            unzipChecked(
                "https://www.wintun.net/builds/wintun-$WINTUN_VERSION.zip", WINTUN_SHA256, out,
                mapOf("wintun/bin/${goarch.get()}/wintun.dll" to "wintun.dll"),
            )
        }
        if ("WinDivert.dll" in windowsFiles && listOf("WinDivert.dll", "WinDivert64.sys").any { !File(out, it).isFile }) {
            val dir = "WinDivert-$WINDIVERT_VERSION-A/x64"
            unzipChecked(
                "https://github.com/basil00/WinDivert/releases/download/v$WINDIVERT_VERSION/WinDivert-$WINDIVERT_VERSION-A.zip",
                WINDIVERT_SHA256, out,
                mapOf("$dir/WinDivert.dll" to "WinDivert.dll", "$dir/WinDivert64.sys" to "WinDivert64.sys"),
            )
        }
    }

    private fun buildCore(out: File) {
        val go = runCatching { run(null, "go", "version") }.getOrNull()
            ?: throw GradleException(
                "Нет ядра OpenFlux в ${out.path}, а Go для его сборки не найден. Установите Go (https://go.dev/dl), " +
                    "соберите ядро scripts/build-core.sh или запустите без него: -PskipCore=true",
            )
        val source = sourceDirs.get().firstOrNull { File(it, "go.mod").isFile } ?: clone()
        logger.lifecycle("Собираю ядро OpenFlux ($coreName) из ${source.path}, $go")
        val target = File(out, coreName)
        exec.exec {
            workingDir = source
            environment("GOOS", goos.get())
            environment("GOARCH", goarch.get())
            environment("CGO_ENABLED", "0")
            environment("GOFLAGS", "-buildvcs=false")
            environment("GOTOOLCHAIN", "auto")
            commandLine("go", "build", "-trimpath", "-ldflags", "-s -w", "-o", target.absolutePath, ".")
        }
        target.setExecutable(true)
        val branch = runCatching { run(source, "git", "rev-parse", "--abbrev-ref", "HEAD") }.getOrNull()
        val rev = runCatching { run(source, "git", "describe", "--always", "--dirty") }.getOrNull()
        File(out, "openflux-core.version").writeText(if (branch != null && rev != null) "$branch@$rev\n" else "local\n")
    }

    private fun clone(): File {
        val dir = cloneDir.get().asFile
        if (File(dir, "go.mod").isFile) return dir
        dir.deleteRecursively()
        val url = "https://github.com/${repository.get()}"
        logger.lifecycle("Скачиваю исходники ядра: $url (${ref.get()})")
        exec.exec { commandLine("git", "clone", "--depth", "1", "-b", ref.get(), url, dir.absolutePath) }
        return dir
    }

    /** Official builds (Wintun, WinDivert), checked against their SHA-256 like in the release workflow. */
    private fun unzipChecked(url: String, sha256: String, out: File, entries: Map<String, String>) {
        val name = url.substringAfterLast('/')
        val zip = URI(url).toURL().readBytes()
        val sha = MessageDigest.getInstance("SHA-256").digest(zip).joinToString("") { "%02x".format(it) }
        if (sha != sha256) throw GradleException("$name: SHA-256 $sha, ожидался $sha256")
        val left = entries.toMutableMap()
        ZipInputStream(zip.inputStream()).use { input ->
            while (left.isNotEmpty()) {
                val e = input.nextEntry ?: throw GradleException("В $name нет ${left.keys.joinToString()}")
                left.remove(e.name)?.let { File(out, it).writeBytes(input.readBytes()) }
            }
        }
    }

    private fun run(dir: File?, vararg command: String): String {
        val stdout = ByteArrayOutputStream()
        exec.exec {
            if (dir != null) workingDir = dir
            commandLine(*command)
            standardOutput = stdout
            errorOutput = ByteArrayOutputStream()
        }
        return stdout.toString().trim()
    }

    private companion object {
        const val WINTUN_VERSION = "0.14.1"
        const val WINTUN_SHA256 = "07c256185d6ee3652e09fa55c0b673e2624b565e02c4b9091c79ca7d2f24ef51"
        const val WINDIVERT_VERSION = "2.2.2"
        const val WINDIVERT_SHA256 = "63cb41763bb4b20f600b6de04e991a9c2be73279e317d4d82f237b150c5f3f15"
    }
}

val hostOs = System.getProperty("os.name").lowercase()
val prepareCore by tasks.registering(PrepareCore::class) {
    description = "Builds the bundled OpenFlux core for this machine when resources/<os> has none"
    val goos = when {
        hostOs.contains("win") -> "windows"
        hostOs.contains("mac") -> "darwin"
        else -> "linux"
    }
    this.goos.set(goos)
    goarch.set(if (System.getProperty("os.arch").lowercase() in setOf("aarch64", "arm64")) "arm64" else "amd64")
    outDir.set(layout.projectDirectory.dir("resources/" + if (goos == "darwin") "macos" else goos))
    cloneDir.set(layout.buildDirectory.dir("openflux-core"))
    sourceDirs.set(
        listOfNotNull(
            (findProperty("coreDir") as String?)?.let(::file),
            rootDir.resolve("OpenFlux"),
            rootDir.resolve("../OpenFlux"),
        ),
    )
    repository.set(providers.gradleProperty("coreRepo").orElse("p1neappleXpress/OpenFlux"))
    ref.set(providers.gradleProperty("coreRef").orElse("main"))
    val skip = findProperty("skipCore") == "true"
    onlyIf { !skip && missing }
}
tasks.matching { it.name == "prepareAppResources" }.configureEach { dependsOn(prepareCore) }

compose.desktop {
    application {
        mainClass = "io.openflux.desktop.MainKt"
        jvmArgs("-Dopenflux.version=$appVersion")
        // JCEF (the built-in browser) reaches into AWT internals.
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")
        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "OpenFlux"
            packageVersion = appVersion
            description = "OpenFlux desktop client"
            vendor = "OpenFlux"
            // The app icon: icons/openflux.svg, drawn as the Android launcher icon.
            // The core (and wintun.dll on Windows) in resources/<windows|macos|linux>,
            // put there by scripts/build-core.sh or the release workflow.
            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            modules("java.instrument", "java.management", "java.net.http", "jdk.crypto.ec", "jdk.unsupported")

            windows {
                menuGroup = "OpenFlux"
                upgradeUuid = "3F0C7B52-9A2E-4C1B-8E77-5D2A6B1E9C40"
                shortcut = true
                perUserInstall = true
                iconFile.set(project.file("icons/openflux.ico"))
            }
            macOS {
                bundleID = "io.openflux.desktop"
                dockName = "OpenFlux"
                appCategory = "public.app-category.utilities"
                iconFile.set(project.file("icons/openflux.icns"))
            }
            linux {
                packageName = "openflux"
                menuGroup = "Network"
                appCategory = "Network"
                shortcut = true
                iconFile.set(project.file("icons/openflux.png"))
            }
        }
    }
}
