package com.youtubearchive.core

import java.nio.file.Path

enum class DownloadMode(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio"),
    BOTH("Video + Audio")
}

enum class SubtitleMode(val label: String) {
    NONE("Nenhuma"),
    AUTO("Automaticas"),
    MANUAL("Manuais"),
    BOTH("Auto + Manuais")
}

data class DownloadRequest(
    val url: String,
    val outputDirectory: Path,
    val mode: DownloadMode = DownloadMode.BOTH,
    val subtitleMode: SubtitleMode = SubtitleMode.BOTH,
    val cutStart: String = "",
    val cutEnd: String = ""
)

data class DownloadPlan(
    val request: DownloadRequest,
    val outputName: String
)

sealed class DownloadStatus {
    data object Idle : DownloadStatus()
    data object Ready : DownloadStatus()
    data object Running : DownloadStatus()
    data object Finished : DownloadStatus()
    data class Failed(val message: String) : DownloadStatus()
}
