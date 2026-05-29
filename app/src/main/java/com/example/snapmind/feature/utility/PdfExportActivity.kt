package com.example.snapmind.feature.utility

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.databinding.ActivityPdfExportBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PdfExportActivity : AppCompatActivity() {
    @Inject lateinit var memoryRepository: MemoryRepository

    private lateinit var binding: ActivityPdfExportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pdfToolbar.setNavigationOnClickListener { finish() }
        binding.pdfSummary.text = "활성 이미지 ${memoryRepository.activeMemories().size}개를 PDF 대상으로 사용할 수 있어요."
        binding.pdfActionButton.setOnClickListener {
            Toast.makeText(this, "PDF 생성 로직 연결 대기 중입니다.", Toast.LENGTH_SHORT).show()
        }
    }
}
