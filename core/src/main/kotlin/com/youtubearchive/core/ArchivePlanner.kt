package com.youtubearchive.core

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

data class ArchiveWorkspace(
    val root: Path,
    val videoDir: Path,
    val audioDir: Path,
    val subtitleDir: Path,
    val cutDir: Path,
    val logDir: Path
)

data class ArchivePlan(
    val request: DownloadRequest,
    val outputName: String,
    val workspace: ArchiveWorkspace,
    val commands: List<String>,
    val generatedAt: Instant
)

data class ArchivePreparation(
    val plan: ArchivePlan,
    val directories: List<Path>,
    val summaryFile: Path,
    val commandsFile: Path,
    val subtitleRequestFile: Path?,
    val cutRequestFile: Path?
)

object ArchivePlanner {
    fun buildPlan(request: DownloadRequest): ArchivePlan {
        val normalizedRequest = request.copy(
            url = request.url.trim()
        )
        require(normalizedRequest.url.isNotEmpty()) { "Informe a URL do video." }
        require(normalizedRequest.outputDirectory.toString().isNotBlank()) { "Informe a pasta de download." }

        val outputName = buildOutputName(normalizedRequest.url)
        val root = normalizedRequest.outputDirectory.normalize().resolve(outputName)
        val workspace = ArchiveWorkspace(
            root = root,
            videoDir = root.resolve("video"),
            audioDir = root.resolve("audio"),
            subtitleDir = root.resolve("subtitles"),
            cutDir = root.resolve("cuts"),
            logDir = root.resolve("logs")
        )

        return ArchivePlan(
            request = normalizedRequest,
            outputName = outputName,
            workspace = workspace,
            commands = buildCommands(normalizedRequest, workspace),
            generatedAt = Instant.now()
        )
    }

    fun prepareArchive(request: DownloadRequest): ArchivePreparation {
        val plan = buildPlan(request)
        val directories = listOf(
            plan.workspace.root,
            plan.workspace.videoDir,
            plan.workspace.audioDir,
            plan.workspace.subtitleDir,
            plan.workspace.cutDir,
            plan.workspace.logDir
        )
        directories.forEach(Files::createDirectories)

        val summaryFile = plan.workspace.logDir.resolve("archive-plan.txt")
        Files.writeString(summaryFile, renderSummary(plan))

        val commandsFile = plan.workspace.logDir.resolve("commands.txt")
        Files.writeString(commandsFile, plan.commands.joinToString(System.lineSeparator() + System.lineSeparator()))

        val subtitleRequestFile = if (plan.request.subtitleMode == SubtitleMode.NONE) {
            null
        } else {
            val file = plan.workspace.subtitleDir.resolve("subtitle-request.txt")
            Files.writeString(file, renderSubtitleRequest(plan))
            file
        }

        val cutRequestFile = if (plan.request.cutStart.isBlank() && plan.request.cutEnd.isBlank()) {
            null
        } else {
            val file = plan.workspace.cutDir.resolve("cut-request.txt")
            Files.writeString(file, renderCutRequest(plan))
            file
        }

        return ArchivePreparation(
            plan = plan,
            directories = directories,
            summaryFile = summaryFile,
            commandsFile = commandsFile,
            subtitleRequestFile = subtitleRequestFile,
            cutRequestFile = cutRequestFile
        )
    }

    fun prepareSubtitleRequest(request: DownloadRequest): Path {
        val plan = buildPlan(request)
        Files.createDirectories(plan.workspace.subtitleDir)
        val file = plan.workspace.subtitleDir.resolve("subtitle-request.txt")
        Files.writeString(file, renderSubtitleRequest(plan))
        return file
    }

    fun prepareCutRequest(request: DownloadRequest): Path {
        val plan = buildPlan(request)
        Files.createDirectories(plan.workspace.cutDir)
        val file = plan.workspace.cutDir.resolve("cut-request.txt")
        Files.writeString(file, renderCutRequest(plan))
        return file
    }

    private fun buildCommands(request: DownloadRequest, workspace: ArchiveWorkspace): List<String> {
        val quotePath = { path: Path -> "\"${path.toAbsolutePath()}\"" }
        val quoteText = { text: String -> "\"${text.replace("\"", "\\\"").trim()}\"" }
        val subtitleFlags = when (request.subtitleMode) {
            SubtitleMode.NONE -> ""
            SubtitleMode.AUTO -> "--write-auto-subs --sub-langs \"all\""
            SubtitleMode.MANUAL -> "--write-subs --sub-langs \"all\""
            SubtitleMode.BOTH -> "--write-subs --write-auto-subs --sub-langs \"all\""
        }

        val baseVideoCommand = buildString {
            append("yt-dlp ")
            if (subtitleFlags.isNotBlank()) {
                append(subtitleFlags)
                append(' ')
            }
            append("-f \"bv*+ba/b\" --merge-output-format mp4 ")
            append("--paths ")
            append(quotePath(workspace.videoDir))
            append(' ')
            append(quoteText(request.url))
        }

        val audioCommand = buildString {
            append("yt-dlp ")
            if (subtitleFlags.isNotBlank()) {
                append(subtitleFlags)
                append(' ')
            }
            append("-x --audio-format mp3 ")
            append("--paths ")
            append(quotePath(workspace.audioDir))
            append(' ')
            append(quoteText(request.url))
        }

        val subtitleCommand = buildString {
            append("yt-dlp --skip-download ")
            when (request.subtitleMode) {
                SubtitleMode.NONE -> append("# legenda desativada")
                SubtitleMode.AUTO -> append("--write-auto-subs --sub-langs \"all\"")
                SubtitleMode.MANUAL -> append("--write-subs --sub-langs \"all\"")
                SubtitleMode.BOTH -> append("--write-subs --write-auto-subs --sub-langs \"all\"")
            }
            append(' ')
            append("--paths ")
            append(quotePath(workspace.subtitleDir))
            append(' ')
            append(quoteText(request.url))
        }

        val cutCommand = if (request.cutStart.isBlank() && request.cutEnd.isBlank()) {
            null
        } else {
            buildString {
                append("ffmpeg ")
                if (request.cutStart.isNotBlank()) {
                    append("-ss ").append(request.cutStart.trim()).append(' ')
                }
                if (request.cutEnd.isNotBlank()) {
                    append("-to ").append(request.cutEnd.trim()).append(' ')
                }
                append("-i ").append(quotePath(workspace.videoDir.resolve("source.mp4"))).append(' ')
                append("-c copy ")
                append(quotePath(workspace.cutDir.resolve("cut-output.mp4")))
            }
        }

        return buildList {
            when (request.mode) {
                DownloadMode.VIDEO -> add(baseVideoCommand)
                DownloadMode.AUDIO -> add(audioCommand)
                DownloadMode.BOTH -> {
                    add(baseVideoCommand)
                    add(audioCommand)
                }
            }

            if (request.subtitleMode != SubtitleMode.NONE) {
                add(subtitleCommand)
            }

            if (cutCommand != null) {
                add(cutCommand)
            }
        }
    }

    private fun renderSummary(plan: ArchivePlan): String = buildString {
        appendLine("YouTube Archive Recorder")
        appendLine("Gerado em: ${plan.generatedAt}")
        appendLine("URL: ${plan.request.url}")
        appendLine("Modo: ${plan.request.mode.label}")
        appendLine("Legendas: ${plan.request.subtitleMode.label}")
        appendLine("Corte inicio: ${plan.request.cutStart.ifBlank { "-" }}")
        appendLine("Corte fim: ${plan.request.cutEnd.ifBlank { "-" }}")
        appendLine("Saida: ${plan.workspace.root}")
        appendLine()
        appendLine("Pastas")
        appendLine("- video: ${plan.workspace.videoDir}")
        appendLine("- audio: ${plan.workspace.audioDir}")
        appendLine("- subtitles: ${plan.workspace.subtitleDir}")
        appendLine("- cuts: ${plan.workspace.cutDir}")
        appendLine("- logs: ${plan.workspace.logDir}")
    }

    private fun renderSubtitleRequest(plan: ArchivePlan): String = buildString {
        appendLine("Legenda solicitada para:")
        appendLine(plan.request.url)
        appendLine()
        appendLine("Modo: ${plan.request.subtitleMode.label}")
        appendLine("Saida: ${plan.workspace.subtitleDir}")
    }

    private fun renderCutRequest(plan: ArchivePlan): String = buildString {
        appendLine("Corte solicitado para:")
        appendLine(plan.request.url)
        appendLine()
        appendLine("Inicio: ${plan.request.cutStart.ifBlank { "-" }}")
        appendLine("Fim: ${plan.request.cutEnd.ifBlank { "-" }}")
        appendLine("Saida: ${plan.workspace.cutDir}")
    }

    private fun buildOutputName(url: String): String {
        val videoId = extractVideoId(url)
        val baseName = when {
            !videoId.isNullOrBlank() -> "youtube-${sanitizeSegment(videoId)}"
            else -> {
                val host = runCatching { URI(url).host }.getOrNull().orEmpty()
                val hostSlug = sanitizeSegment(host.ifBlank { "video" })
                val hashSuffix = url.hashCode().toUInt().toString(16)
                "youtube-$hostSlug-$hashSuffix"
            }
        }

        return baseName.take(80).trim('-').ifBlank { "youtube-archive" }
    }

    private fun extractVideoId(url: String): String? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val query = uri.rawQuery.orEmpty()
        query.split('&').forEach { token ->
            val pair = token.split('=', limit = 2)
            if (pair.size == 2 && pair[0] == "v") {
                return decodeQueryComponent(pair[1])
            }
        }

        val pathSegments = uri.path.split('/').filter(String::isNotBlank)
        if (pathSegments.size >= 2 && pathSegments[0] == "shorts") {
            return sanitizeSegment(pathSegments[1])
        }
        if (pathSegments.size >= 2 && pathSegments[0] == "embed") {
            return sanitizeSegment(pathSegments[1])
        }
        if (pathSegments.size >= 2 && pathSegments[0] == "live") {
            return sanitizeSegment(pathSegments[1])
        }

        return pathSegments.lastOrNull()?.takeIf { it.isNotBlank() }?.let(::sanitizeSegment)
    }

    private fun sanitizeSegment(value: String): String {
        val normalized = value
            .replace(Regex("[^A-Za-z0-9]+"), "-")
            .trim('-')
        return normalized.ifBlank { "video" }
    }

    private fun decodeQueryComponent(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8)
    }
}
