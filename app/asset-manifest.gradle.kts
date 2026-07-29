import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import java.security.MessageDigest

abstract class GenerateAssetManifestTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resourceDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val versionFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archiveFiles: ConfigurableFileCollection

    @get:Input
    abstract val clientIds: ListProperty<String>

    @get:OutputFile
    abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val resourceDir = resourceDirectory.get().asFile
        val archivesByName = archiveFiles.files.associateBy { it.nameWithoutExtension }
        val versionSha = versionFile.get().asFile.sha256()
        val descriptors = buildList {
            val coreFiles = resourceDir.walkTopDown()
                .filter { file ->
                    file.isFile && !file.relativeTo(resourceDir).invariantSeparatorsPath.startsWith("global/")
                }
                .toList()
            add(descriptor("core", "", requireNotNull(archivesByName["core"]), coreFiles, versionSha))
            clientIds.get().forEach { client ->
                val clientFiles = File(resourceDir, "global/$client")
                    .walkTopDown()
                    .filter(File::isFile)
                    .toList()
                add(
                    descriptor(
                        client,
                        "global/$client",
                        requireNotNull(archivesByName[client]),
                        clientFiles,
                        versionSha,
                    )
                )
            }
        }
        manifestFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("""{"archives":[${descriptors.joinToString(",")}] }""".replace("] }", "]}"))
        }
    }

    private fun descriptor(
        id: String,
        destination: String,
        archive: File,
        sourceFiles: List<File>,
        versionSha: String,
    ): String =
        """{"id":"$id","assetPath":"MaaSync/archives/$id.zip","destination":"$destination","sha256":"${archive.sha256()}","fileCount":${sourceFiles.size},"uncompressedSize":${sourceFiles.sumOf { it.length() }},"versionSha256":"$versionSha"}"""

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

val maaAssetsDir = layout.projectDirectory.dir("src/main/assets")
val maaResourceDir = maaAssetsDir.dir("MaaSync/MaaResource")
val generatedArchiveDir = layout.buildDirectory.dir("generated/assets/MaaSync/archives")
val globalClients = listOf("YoStarEN", "YoStarJP", "YoStarKR", "txwy")

val generateCoreAssetArchive by tasks.registering(Zip::class) {
    from(maaResourceDir) {
        exclude("global/**")
    }
    archiveFileName.set("core.zip")
    destinationDirectory.set(generatedArchiveDir)
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

val globalArchiveTasks = globalClients.associateWith { client ->
    tasks.register<Zip>("generate${client.replaceFirstChar(Char::uppercase)}AssetArchive") {
        from(maaResourceDir.dir("global/$client"))
        archiveFileName.set("$client.zip")
        destinationDirectory.set(generatedArchiveDir)
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

val generateAssetManifest by tasks.registering(GenerateAssetManifestTask::class) {
    resourceDirectory.set(maaResourceDir)
    versionFile.set(maaResourceDir.file("version.json"))
    archiveFiles.from(generateCoreAssetArchive.flatMap { it.archiveFile })
    globalArchiveTasks.values.forEach { task ->
        archiveFiles.from(task.flatMap { it.archiveFile })
    }
    clientIds.set(globalClients)
    manifestFile.set(layout.buildDirectory.file("generated/assets/MaaSync/asset_manifest.json"))
    dependsOn(generateCoreAssetArchive)
    dependsOn(globalArchiveTasks.values)
}

val prepareStaticAssets by tasks.registering(Copy::class) {
    from(maaAssetsDir) {
        exclude("MaaSync/MaaResource/**")
        exclude("MaaSync/asset_manifest.json")
    }
    into(layout.buildDirectory.dir("generated/static-assets"))
}

tasks.matching { it.name.startsWith("preBuild") }.configureEach {
    dependsOn(generateAssetManifest)
    dependsOn(prepareStaticAssets)
}
