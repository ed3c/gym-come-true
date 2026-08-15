package dev.ed3c.gymcometrue.scan

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import dev.ed3c.gymcometrue.domain.ScanEvidence
import dev.ed3c.gymcometrue.domain.SupplementLabelParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidLabelScanner(
    private val context: Context,
) {
    data class Result(
        val evidence: ScanEvidence,
        val barcode: String?,
    )

    suspend fun scan(file: File): Result {
        val textRecognizer = TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build(),
        )
        val barcodeScanner = BarcodeScanning.getClient()

        return try {
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            val textResult = textRecognizer.process(image).await()
            val barcodeResult = barcodeScanner.process(image).await()
            val barcode = barcodeResult.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
            val rawText = textResult.text
            val candidates = SupplementLabelParser.parse(rawText)
            val warnings = buildList {
                add("OCR output is unverified and must be checked against the physical label.")
                if (rawText.isBlank()) add("No readable label text was found.")
                if (candidates.isEmpty()) add("No ingredient-and-amount pair was parsed.")
                if (barcode == null) add("No barcode was detected; product identity remains unresolved.")
            }
            val evidence = ScanEvidence(
                rawTextSha256 = rawText.sha256(),
                barcode = barcode,
                candidates = candidates,
                warnings = warnings,
            )
            Result(evidence = evidence, barcode = barcode)
        } finally {
            textRecognizer.close()
            barcodeScanner.close()
            runCatching { file.delete() }
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value ->
        if (continuation.isActive) continuation.resume(value)
    }
    addOnFailureListener { error ->
        if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener {
        if (continuation.isActive) continuation.resumeWithException(CancellationException("ML Kit task cancelled"))
    }
}

private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray(Charsets.UTF_8))
    .joinToString(separator = "") { byte -> "%02x".format(byte) }
