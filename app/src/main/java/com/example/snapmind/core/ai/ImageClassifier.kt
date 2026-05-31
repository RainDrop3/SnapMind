package com.example.snapmind.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import com.example.snapmind.core.image.BitmapDecoder
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.example.snapmind.core.coroutine.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter

data class ClassificationPrediction(
    val label: String,
    val confidence: Float,
    val rank: Int,
)

data class ClassificationResult(
    val predictions: List<ClassificationPrediction>,
    val modelVersion: String,
) {
    val top: ClassificationPrediction? get() = predictions.firstOrNull()
}

class ModelUnavailableException(message: String) : IOException(message)

@Singleton
class ImageClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
) {

    @Volatile private var interpreter: Interpreter? = null
    @Volatile private var labels: List<String> = emptyList()
    @Volatile private var loadFailed: Boolean = false

    suspend fun classify(imageUri: Uri): Result<ClassificationResult> =
        withContext(dispatcherProvider.default) {
            runCatching {
                val interp = obtainInterpreter()
                val activeLabels = labels
                val bitmap = loadBitmap(imageUri)
                val input = preprocess(bitmap)
                val output = Array(1) { FloatArray(activeLabels.size) }
                interp.run(input, output)
                val scores = output[0]
                val ranked = scores.withIndex()
                    .sortedByDescending { it.value }
                    .take(TOP_K)
                    .mapIndexed { index, indexed ->
                        ClassificationPrediction(
                            label = activeLabels.getOrElse(indexed.index) { LABEL_UNKNOWN },
                            confidence = indexed.value,
                            rank = index + 1,
                        )
                    }
                val top = ranked.firstOrNull()
                val effectiveTop = if (top != null && top.confidence < CONFIDENCE_THRESHOLD) {
                    ClassificationPrediction(LABEL_UNKNOWN, top.confidence, 1)
                } else {
                    top
                }
                val final = if (effectiveTop != null && effectiveTop.label != ranked.firstOrNull()?.label) {
                    listOf(effectiveTop) + ranked.drop(1).mapIndexed { idx, p ->
                        p.copy(rank = idx + 2)
                    }
                } else ranked
                ClassificationResult(predictions = final, modelVersion = MODEL_VERSION)
            }.onFailure { error ->
                Log.e(TAG, "Classification failed for $imageUri", error)
            }
        }

    private fun obtainInterpreter(): Interpreter {
        interpreter?.let { return it }
        if (loadFailed) throw ModelUnavailableException("Model previously failed to load")
        synchronized(this) {
            interpreter?.let { return it }
            try {
                val fd = context.assets.openFd(MODEL_ASSET)
                fd.use { afd ->
                    val channel = afd.createInputStream().channel
                    val byteBuffer = channel.map(
                        java.nio.channels.FileChannel.MapMode.READ_ONLY,
                        afd.startOffset,
                        afd.declaredLength,
                    )
                    val options = Interpreter.Options().apply { setNumThreads(2) }
                    val loaded = Interpreter(byteBuffer, options)
                    labels = loadLabels()
                    interpreter = loaded
                    return loaded
                }
            } catch (e: IOException) {
                loadFailed = true
                Log.e(TAG, "Model asset '$MODEL_ASSET' missing", e)
                throw ModelUnavailableException("Model asset '$MODEL_ASSET' missing: ${e.message}")
            }
        }
    }

    private fun loadLabels(): List<String> {
        return try {
            context.assets.open(LABELS_ASSET).bufferedReader().use { reader ->
                reader.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toList()
            }.also {
                Log.i(TAG, "Loaded ${it.size} labels from $LABELS_ASSET: $it")
            }
        } catch (e: IOException) {
            Log.w(TAG, "Labels asset '$LABELS_ASSET' missing — falling back to hardcoded labels", e)
            FALLBACK_LABELS
        }
    }

    private fun loadBitmap(uri: Uri): Bitmap {
        val raw = BitmapDecoder.decodeSampled(
            contentResolver = context.contentResolver,
            uri = uri,
            targetWidth = INPUT_SIZE,
            targetHeight = INPUT_SIZE,
        ) ?: throw IOException("Decode failed for $uri")
        val rotation = readExifRotation(uri)
        val rotated = if (rotation == 0) raw else applyRotation(raw, rotation)
        return if (rotated.width == INPUT_SIZE && rotated.height == INPUT_SIZE) rotated
        else Bitmap.createScaledBitmap(rotated, INPUT_SIZE, INPUT_SIZE, true)
    }

    private fun readExifRotation(uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
    }.getOrDefault(0)

    private fun applyRotation(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        // EfficientNet 계열은 모델 내부에 정규화가 포함되어 있어 입력을 0~255 float 그대로 넣어야 함.
        // (snapmind_trainmodel.py 참조)
        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * CHANNELS)
            .order(ByteOrder.nativeOrder())
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF).toFloat()
            val g = ((pixel shr 8) and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()
            buffer.putFloat(r)
            buffer.putFloat(g)
            buffer.putFloat(b)
        }
        buffer.rewind()
        return buffer
    }

    companion object {
        const val MODEL_ASSET = "image_classifier_v1_0_0.tflite"
        const val LABELS_ASSET = "labels.txt"
        const val MODEL_VERSION = "v1.0.0"
        private const val TAG = "ImageClassifier"
        private const val INPUT_SIZE = 224
        private const val CHANNELS = 3
        private const val TOP_K = 3
        private const val CONFIDENCE_THRESHOLD = 0.65f
        private const val LABEL_UNKNOWN = "unknown"

        // labels.txt가 없을 때 폴백. 모델 학습 시 image_dataset_from_directory의
        // 알파벳 정렬 순서를 따른다는 가정으로 정렬되어 있음.
        private val FALLBACK_LABELS: List<String> = listOf(
            "chat",
            "code",
            "document",
            "food",
            "receipt",
            "shopping",
            "travel",
            "unknown",
            "youtube",
        )
    }
}
