package com.example.snapmind.feature.utility

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.databinding.ActivityPdfExportBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PdfExportActivity : AppCompatActivity() {
    @Inject lateinit var memoryRepository: MemoryRepository

    private lateinit var binding: ActivityPdfExportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pdfToolbar.setNavigationOnClickListener { finish() }
        val active = memoryRepository.activeMemories()
        binding.pdfSummary.text = "활성 이미지 ${active.size}개를 PDF 대상으로 사용할 수 있어요."
        binding.pdfActionButton.setOnClickListener { exportAndShare() }
    }

    private fun exportAndShare() {
        val ids = memoryRepository.activeMemories().map { it.id }
        if (ids.isEmpty()) {
            Toast.makeText(this, "내보낼 항목이 없어요.", Toast.LENGTH_SHORT).show()
            return
        }
        binding.pdfActionButton.isEnabled = false
        binding.pdfSummary.text = "PDF 생성 중…"
        lifecycleScope.launch {
            when (val result = memoryRepository.exportToPdf(ids)) {
                is AppResult.Success -> {
                    binding.pdfSummary.text = "활성 이미지 ${ids.size}개를 PDF로 추출했어요."
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, result.data)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(share, "SnapMind PDF 공유"))
                }
                is AppResult.Error -> {
                    binding.pdfSummary.text = "PDF 생성에 실패했어요."
                    Toast.makeText(
                        this@PdfExportActivity,
                        "PDF 생성 실패: ${result.error}",
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
            binding.pdfActionButton.isEnabled = true
        }
    }
}
