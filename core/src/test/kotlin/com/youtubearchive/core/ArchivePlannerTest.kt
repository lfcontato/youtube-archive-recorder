package com.youtubearchive.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path

class ArchivePlannerTest {
    private val tempDirectories = mutableListOf<Path>()

    @AfterTest
    fun cleanup() {
        tempDirectories.forEach { directory ->
            runCatching { directory.toFile().deleteRecursively() }
        }
        tempDirectories.clear()
    }

    @Test
    fun buildPlanDerivesStableOutputNameFromVideoId() {
        val outputDirectory = createTempDirectory()
        val request = DownloadRequest(
            url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            outputDirectory = outputDirectory,
            mode = DownloadMode.BOTH,
            subtitleMode = SubtitleMode.BOTH
        )

        val plan = ArchivePlanner.buildPlan(request)

        assertEquals("youtube-dQw4w9WgXcQ", plan.outputName)
        assertTrue(plan.workspace.root.startsWith(outputDirectory))
        assertTrue(plan.commands.isNotEmpty())
    }

    @Test
    fun prepareArchiveWritesPlanFilesAndDirectories() {
        val outputDirectory = createTempDirectory()
        val request = DownloadRequest(
            url = "https://youtu.be/dQw4w9WgXcQ",
            outputDirectory = outputDirectory,
            mode = DownloadMode.VIDEO,
            subtitleMode = SubtitleMode.AUTO,
            cutStart = "00:00:10",
            cutEnd = "00:00:25"
        )

        val preparation = ArchivePlanner.prepareArchive(request)

        assertTrue(Files.isDirectory(preparation.plan.workspace.root))
        assertTrue(Files.isDirectory(preparation.plan.workspace.videoDir))
        assertTrue(Files.isDirectory(preparation.plan.workspace.audioDir))
        assertTrue(Files.isDirectory(preparation.plan.workspace.subtitleDir))
        assertTrue(Files.isDirectory(preparation.plan.workspace.cutDir))
        assertTrue(Files.isDirectory(preparation.plan.workspace.logDir))
        assertTrue(Files.exists(preparation.summaryFile))
        assertTrue(Files.exists(preparation.commandsFile))
        assertNotNull(preparation.subtitleRequestFile)
        assertNotNull(preparation.cutRequestFile)
        assertTrue(Files.readString(preparation.summaryFile).contains("YouTube Archive Recorder"))
    }

    @Test
    fun prepareSubtitleRequestCreatesSubtitleSpec() {
        val outputDirectory = createTempDirectory()
        val request = DownloadRequest(
            url = "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            outputDirectory = outputDirectory,
            subtitleMode = SubtitleMode.MANUAL
        )

        val file = ArchivePlanner.prepareSubtitleRequest(request)

        assertTrue(Files.exists(file))
        assertTrue(Files.readString(file).contains("Legenda solicitada"))
    }

    private fun createTempDirectory(): Path {
        val directory = Files.createTempDirectory("archive-planner-test")
        tempDirectories.add(directory)
        return directory
    }
}
