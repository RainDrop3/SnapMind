package com.example.snapmind.core.ai

import android.content.Context
import android.net.Uri
import com.example.snapmind.core.coroutine.DispatcherProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class OcrResult(
    val fullText: String,
    val rawText: String,
)

@Singleton
class OcrExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {

    // 한국어 인식기는 한글과 함께 라틴 문자·숫자도 인식하므로 한글 스크린샷이 깨지지 않는다.
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    suspend fun extract(imageUri: Uri): Result<OcrResult> = withContext(dispatcherProvider.io) {
        runCatching {
            val image = InputImage.fromFilePath(context, imageUri)
            val visionText = suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
                cont.invokeOnCancellation { /* recognizer handles its own cancel */ }
            }
            val raw = visionText.text
            OcrResult(
                fullText = normalize(raw),
                rawText = raw,
            )
        }
    }

    private fun normalize(raw: String): String =
        raw.lineSequence()
            .map { it.trim() }
            .fold(StringBuilder()) { acc, line ->
                if (line.isEmpty()) {
                    if (acc.isNotEmpty() && !acc.endsWith("\n\n")) acc.append('\n')
                } else {
                    if (acc.isNotEmpty() && !acc.endsWith('\n')) acc.append('\n')
                    acc.append(line)
                }
                acc
            }
            .toString()
            .trim()
}
