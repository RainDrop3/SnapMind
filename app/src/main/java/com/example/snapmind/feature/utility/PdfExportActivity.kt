package com.example.snapmind.feature.utility

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.databinding.ActivityPdfExportBinding
import com.example.snapmind.feature.home.MemoryGridAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PdfExportActivity : AppCompatActivity() {
    @Inject lateinit var memoryRepository: MemoryRepository

    private lateinit var binding: ActivityPdfExportBinding
    private lateinit var adapter: MemoryGridAdapter

    private val selectedIds = linkedSetOf<Long>()
    private var pendingSaveIds: List<Long> = emptyList()

    private val createDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            if (uri == null) {
                pendingSaveIds = emptyList()
                return@registerForActivityResult
            }
            saveToUri(uri, pendingSaveIds)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfExportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.pdfToolbar.setNavigationOnClickListener { finish() }

        // 홈의 꾹 눌러 선택하는 UI와 동일한 카드/선택 디자인을 재사용한다.
        adapter = MemoryGridAdapter(
            onMemoryClick = { toggleSelection(it.id) },
            onActionClick = { toggleSelection(it.id) },
            onMemoryLongClick = { toggleSelection(it.id); true },
        )
        binding.pdfRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.pdfRecyclerView.adapter = adapter

        val active = memoryRepository.activeMemories()
        adapter.submitList(active)
        binding.pdfRecyclerView.visibility = if (active.isEmpty()) View.GONE else View.VISIBLE
        binding.pdfEmptyState.visibility = if (active.isEmpty()) View.VISIBLE else View.GONE
        binding.pdfSelectAllButton.isEnabled = active.isNotEmpty()
        updateSelectionUi()

        binding.pdfSelectAllButton.setOnClickListener { toggleSelectAll() }
        binding.pdfShareButton.setOnClickListener { shareSelected() }
        binding.pdfSaveButton.setOnClickListener { startSaveSelected() }
    }

    private fun toggleSelection(memoryId: Long) {
        if (!selectedIds.add(memoryId)) selectedIds.remove(memoryId)
        adapter.setSelectedIds(selectedIds)
        updateSelectionUi()
    }

    private fun toggleSelectAll() {
        val all = adapter.currentList.map { it.id }
        if (selectedIds.size == all.size) {
            selectedIds.clear()
        } else {
            selectedIds.clear()
            selectedIds.addAll(all)
        }
        adapter.setSelectedIds(selectedIds)
        updateSelectionUi()
    }

    private fun updateSelectionUi() {
        val total = adapter.currentList.size
        val count = selectedIds.size
        binding.pdfSummary.text = "활성 이미지 ${total}개 · 선택 ${count}개"
        binding.pdfShareButton.isEnabled = count > 0
        binding.pdfSaveButton.isEnabled = count > 0
        binding.pdfSelectAllButton.text =
            if (count == total && total > 0) "선택 해제" else "전체 선택"
    }

    private fun shareSelected() {
        val ids = selectedIds.toList()
        if (ids.isEmpty()) return
        setBusy(true)
        lifecycleScope.launch {
            when (val result = memoryRepository.exportToPdf(ids)) {
                is AppResult.Success -> {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, result.data)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(share, "SnapMind PDF 공유"))
                }
                is AppResult.Error -> toast("PDF 생성 실패: ${result.error}")
            }
            setBusy(false)
        }
    }

    private fun startSaveSelected() {
        val ids = selectedIds.toList()
        if (ids.isEmpty()) return
        pendingSaveIds = ids
        val name = "snapmind_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())}.pdf"
        createDocument.launch(name)
    }

    private fun saveToUri(target: Uri, ids: List<Long>) {
        if (ids.isEmpty()) return
        setBusy(true)
        lifecycleScope.launch {
            when (val result = memoryRepository.exportToPdf(ids)) {
                is AppResult.Success -> {
                    val copied = runCatching {
                        contentResolver.openOutputStream(target)?.use { output ->
                            contentResolver.openInputStream(result.data)?.use { input ->
                                input.copyTo(output)
                            } ?: error("source stream unavailable")
                        } ?: error("target stream unavailable")
                    }.isSuccess
                    toast(if (copied) "${ids.size}개 항목을 PDF로 저장했어요." else "PDF 저장에 실패했어요.")
                }
                is AppResult.Error -> toast("PDF 생성 실패: ${result.error}")
            }
            pendingSaveIds = emptyList()
            setBusy(false)
        }
    }

    private fun setBusy(busy: Boolean) {
        val hasSelection = selectedIds.isNotEmpty()
        binding.pdfShareButton.isEnabled = !busy && hasSelection
        binding.pdfSaveButton.isEnabled = !busy && hasSelection
        binding.pdfSelectAllButton.isEnabled = !busy && adapter.currentList.isNotEmpty()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
