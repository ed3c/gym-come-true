package dev.ed3c.gymcometrue

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dev.ed3c.gymcometrue.reminder.ProtocolReminderScheduler
import dev.ed3c.gymcometrue.scan.AndroidLabelScanner
import dev.ed3c.gymcometrue.ui.GymComeTrueApp
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val scope = rememberCoroutineScope()
            val scanner = remember { AndroidLabelScanner(applicationContext) }
            var pendingScanFile by remember { mutableStateOf<File?>(null) }
            var scanSummary by remember { mutableStateOf<String?>(null) }

            val takePicture = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture(),
            ) { captured ->
                val file = pendingScanFile
                pendingScanFile = null

                if (!captured || file == null) {
                    file?.delete()
                    scanSummary = "Scan cancelled; no image was retained."
                } else {
                    scope.launch {
                        scanSummary = runCatching { scanner.scan(file).toSummary() }
                            .getOrElse { error ->
                                "Label extraction failed: ${error.message ?: error::class.simpleName}. The temporary image was deleted."
                            }
                    }
                }
            }

            val notificationPermission = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
            ) { granted ->
                if (granted) {
                    ProtocolReminderScheduler.schedule(applicationContext, delayMillis = 60_000L)
                }
            }

            GymComeTrueApp(
                platformName = "Android",
                scanSummary = scanSummary,
                onScanLabel = {
                    val directory = File(cacheDir, "label-scans").apply { mkdirs() }
                    val file = File.createTempFile("label-", ".jpg", directory)
                    pendingScanFile = file
                    val uri: Uri = FileProvider.getUriForFile(
                        this,
                        "$packageName.fileprovider",
                        file,
                    )
                    takePicture.launch(uri)
                },
                onScheduleNextReminder = {
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED

                    if (needsPermission) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        ProtocolReminderScheduler.schedule(applicationContext, delayMillis = 60_000L)
                    }
                },
            )
        }
    }
}

private fun AndroidLabelScanner.Result.toSummary(): String = buildString {
    append("Detected ")
    append(evidence.candidates.size)
    append(" ingredient candidates")
    barcode?.let { append(" and barcode ").append(it.take(32)) }
    append(". Evidence hash: ")
    append(evidence.rawTextSha256.take(12))
    append("… Confirm every field before logging; no dose was calculated.")
}
