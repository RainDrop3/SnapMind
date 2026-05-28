package com.example.snapmind.feature.importimage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.snapmind.MainActivity
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.databinding.ActivityShareImportBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShareActivity : AppCompatActivity() {
    @Inject lateinit var memoryRepository: MemoryRepository

    private lateinit var binding: ActivityShareImportBinding
    private val sharedUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedUris += intent.imageUris()
        if (sharedUris.isEmpty()) {
            Toast.makeText(this, "이미지 공유만 지원합니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        renderPreview()
        binding.shareToolbar.setNavigationOnClickListener { finish() }
        binding.cancelButton.setOnClickListener { finish() }
        binding.saveButton.setOnClickListener { saveSharedImages() }
    }

    private fun renderPreview() = with(binding) {
        val firstUri = sharedUris.first()
        Glide.with(sharePreview)
            .load(firstUri)
            .thumbnail(0.25f)
            .centerCrop()
            .into(sharePreview)
        val mimeType = contentResolver.getType(firstUri) ?: intent.type ?: "image/*"
        shareMeta.text = "공유 이미지 ${sharedUris.size}개 · $mimeType"
    }

    private fun saveSharedImages() {
        binding.saveButton.isEnabled = false
        lifecycleScope.launch {
            var successCount = 0
            sharedUris.forEach { uri ->
                val result = memoryRepository.importImage(
                    sourceUri = uri,
                    mimeType = contentResolver.getType(uri) ?: intent.type,
                    sourceLabel = callingPackage ?: "공유 이미지",
                )
                if (result is AppResult.Success) {
                    successCount += 1
                }
            }

            if (successCount == 0) {
                binding.saveButton.isEnabled = true
                Toast.makeText(this@ShareActivity, "저장할 수 있는 이미지가 없어요.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ShareActivity, "${successCount}개 이미지를 저장했어요.", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this@ShareActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
                finish()
            }
        }
    }

    private fun Intent.imageUris(): List<Uri> =
        when (action) {
            Intent.ACTION_SEND -> listOfNotNull(getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            Intent.ACTION_SEND_MULTIPLE -> getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
            else -> emptyList()
        }
}
