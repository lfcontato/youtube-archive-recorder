package com.youtubearchive.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.youtubearchive.core.ArchivePlanner
import com.youtubearchive.core.DownloadMode
import com.youtubearchive.core.DownloadRequest
import com.youtubearchive.core.DownloadStatus
import com.youtubearchive.core.SubtitleMode
import java.nio.file.Path
import kotlin.io.path.createDirectories

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "YouTube Archive Recorder"
    ) {
        MaterialTheme {
            YouTubeArchiveApp()
        }
    }
}

@Composable
private fun YouTubeArchiveApp() {
    val defaultOutput = remember { defaultDownloadDirectory() }

    var url by remember { mutableStateOf("") }
    var outputDirectory by remember { mutableStateOf(defaultOutput.toString()) }
    var cutStart by remember { mutableStateOf("") }
    var cutEnd by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<DownloadStatus>(DownloadStatus.Idle) }
    var selectedMode by remember { mutableStateOf(DownloadMode.BOTH) }
    var selectedSubtitleMode by remember { mutableStateOf(SubtitleMode.BOTH) }
    var details by remember { mutableStateOf("Prepare um pacote local para o video, com comandos e pastas organizadas.") }

    LaunchedEffect(Unit) {
        status = DownloadStatus.Ready
    }

    val appShellBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF08111F),
            Color(0xFF111B2F),
            Color(0xFF1A273E)
        )
    )

    val requestBuilder = {
        DownloadRequest(
            url = url,
            outputDirectory = Path.of(outputDirectory).apply { createDirectories() },
            mode = selectedMode,
            subtitleMode = selectedSubtitleMode,
            cutStart = cutStart,
            cutEnd = cutEnd
        )
    }

    fun runAction(actionName: String, block: (DownloadRequest) -> String) {
        status = DownloadStatus.Running
        runCatching { block(requestBuilder()) }
            .onSuccess {
                details = it
                status = DownloadStatus.Finished
            }
            .onFailure { error ->
                status = DownloadStatus.Failed(error.message ?: "Falha inesperada.")
                details = "$actionName nao foi concluido."
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appShellBrush)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "YouTube Archive Recorder",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Baixe videos, extraia legendas e prepare cortes locais em uma interface simples para Windows.",
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.bodyMedium
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121C2E)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("URL do YouTube") }
                    )
                    OutlinedTextField(
                        value = outputDirectory,
                        onValueChange = { outputDirectory = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pasta de download") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = cutStart,
                            onValueChange = { cutStart = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Corte inicio") }
                        )
                        OutlinedTextField(
                            value = cutEnd,
                            onValueChange = { cutEnd = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Corte fim") }
                        )
                    }
                    HorizontalDivider(color = Color(0xFF27364F))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { selectedMode = DownloadMode.VIDEO }) { Text("Video") }
                        Button(onClick = { selectedMode = DownloadMode.AUDIO }) { Text("Audio") }
                        Button(onClick = { selectedMode = DownloadMode.BOTH }) { Text("Video + Audio") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { selectedSubtitleMode = SubtitleMode.NONE }) { Text("Sem legenda") }
                        Button(onClick = { selectedSubtitleMode = SubtitleMode.AUTO }) { Text("Auto") }
                        Button(onClick = { selectedSubtitleMode = SubtitleMode.MANUAL }) { Text("Manual") }
                        Button(onClick = { selectedSubtitleMode = SubtitleMode.BOTH }) { Text("Auto + Manual") }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        runAction("Preparacao do download") { request ->
                            val preparation = ArchivePlanner.prepareArchive(request)
                            buildString {
                                appendLine("Pacote preparado em: ${preparation.plan.workspace.root}")
                                appendLine("Planilha: ${preparation.summaryFile}")
                                appendLine("Comandos: ${preparation.commandsFile}")
                                preparation.subtitleRequestFile?.let {
                                    appendLine("Legenda: $it")
                                }
                                preparation.cutRequestFile?.let {
                                    appendLine("Corte: $it")
                                }
                                appendLine()
                                appendLine("Comandos sugeridos:")
                                preparation.plan.commands.forEach { command ->
                                    appendLine(command)
                                }
                            }
                        }
                    }
                ) {
                    Text("Preparar download")
                }

                Button(
                    onClick = {
                        runAction("Geracao do corte") { request ->
                            val file = ArchivePlanner.prepareCutRequest(request)
                            "Especificacao de corte criada em $file"
                        }
                    }
                ) {
                    Text("Criar corte")
                }

                Button(
                    onClick = {
                        runAction("Geracao das legendas") { request ->
                            val file = ArchivePlanner.prepareSubtitleRequest(request)
                            "Especificacao de legendas criada em $file"
                        }
                    }
                ) {
                    Text("Baixar legendas")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1726)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Status", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(statusLabel(status), color = Color(0xFFCBD5E1))
                    Spacer(Modifier.height(10.dp))
                    Text("Detalhes", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(details, color = Color(0xFFCBD5E1))
                }
            }
        }
    }
}

private fun defaultDownloadDirectory(): Path {
    val home = Path.of(System.getProperty("user.home"))
    return home.resolve("Videos").resolve("youtube-archive-recorder")
}

private fun statusLabel(status: DownloadStatus): String = when (status) {
    DownloadStatus.Idle -> "Aguardando"
    DownloadStatus.Ready -> "Pronto para configurar download."
    DownloadStatus.Running -> "Executando operacao..."
    DownloadStatus.Finished -> "Operacao concluida."
    is DownloadStatus.Failed -> "Falha: ${status.message}"
}
