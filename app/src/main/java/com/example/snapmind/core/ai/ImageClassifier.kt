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

                // 모델 출력 = 길이 8의 softmax 확률 배열.
                // (각 카테고리에 대한 확신도(0~1), 전체 합 ≈ 1.0)
                // 이 모델에는 더 이상 'unknown' 클래스가 없다 — 8개 실제 카테고리만 출력한다.
                val output = Array(1) { FloatArray(activeLabels.size) }
                interp.run(input, output)
                val scores = output[0]

                // 확률 내림차순 정렬 → 상위 TOP_K 후보 생성. (rank 1 = 가장 확신하는 카테고리)
                val ranked = scores.withIndex()
                    .sortedByDescending { it.value }
                    .take(TOP_K)
                    .mapIndexed { index, indexed ->
                        ClassificationPrediction(
                            // 출력 인덱스 → labels.txt의 카테고리 이름으로 매핑
                            label = activeLabels.getOrElse(indexed.index) { LABEL_UNKNOWN },
                            confidence = indexed.value,
                            rank = index + 1,
                        )
                    }

                // ── Confidence Thresholding (앱단 후처리) ──────────────────────────
                // 모델은 항상 8개 중 하나를 "가장 그럴듯한" 답으로 내놓는다. 하지만 그 확신도가
                // 낮다면 사실상 "어디에도 속하지 않는" 이미지일 가능성이 크다.
                // 따라서 최고 확률(top-1)이 임계값(0.65) 미만이면 카테고리를 "Unknown"으로 판정하고,
                // 임계값 이상일 때만 모델이 예측한 카테고리 이름을 그대로 반환한다.
                // [진단용] 실제 모델 확률 분포를 로그로 확인. (원인 파악 후 제거 가능)
                //   - top-1 확신도가 0.65 미만이면 임계값에 걸려 Unknown으로 판정됨.
                //   - 학습 데이터인데도 확신도가 낮다면 → 모델 미학습/전처리 불일치 의심.
                Log.d(
                    TAG,
                    "raw scores=" + ranked.joinToString { "${it.label}=%.3f".format(it.confidence) } +
                        " | labelCount=${activeLabels.size}",
                )

                val best = ranked.firstOrNull()
                val predictions = if (best != null && best.confidence < CONFIDENCE_THRESHOLD) {
                    // top-1 라벨만 "Unknown"으로 치환(원본 확신도는 보존),
                    // 나머지 후보(rank 2·3)는 디버그/참고용으로 원본 그대로 둔다.
                    listOf(best.copy(label = LABEL_UNKNOWN)) + ranked.drop(1)
                } else {
                    // 임계값 이상 → 모델 예측 카테고리를 신뢰하고 그대로 사용.
                    ranked
                }

                ClassificationResult(predictions = predictions, modelVersion = MODEL_VERSION)
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
        const val MODEL_ASSET = "image_classifier_v2_0_0.tflite"
        const val LABELS_ASSET = "labels.txt"
        const val MODEL_VERSION = "v1.0.0"
        private const val TAG = "ImageClassifier"
        private const val INPUT_SIZE = 224
        private const val CHANNELS = 3
        private const val TOP_K = 3

        // 최고 확률이 이 값 미만이면 "Unknown"으로 판정하는 임계값(Confidence Threshold).
        // (ml-training-spec.md의 Android Runtime Contract 기본값과 동일)
        private const val CONFIDENCE_THRESHOLD = 0.65f

        // 임계값 미만일 때 부여하는 라벨. 대소문자와 무관하게
        // EntityMappers.toMemoryCategory()가 uppercase() 후 MemoryCategory.UNKNOWN으로 매핑한다.
        private const val LABEL_UNKNOWN = "Unknown"

        // labels.txt가 없을 때 폴백. 모델은 'unknown' 클래스 없이 8개만 출력하며,
        // image_dataset_from_directory의 폴더명 알파벳 정렬 순서와 인덱스가 일치해야 한다.
        private val FALLBACK_LABELS: List<String> = listOf(
            "chat",
            "code",
            "document",
            "food",
            "receipt",
            "shopping",
            "travel",
            "youtube",
        )
    }
}
