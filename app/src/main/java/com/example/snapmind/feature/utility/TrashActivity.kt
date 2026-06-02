package com.example.snapmind.feature.utility

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.snapmind.R
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.databinding.ActivityTrashBinding
import com.example.snapmind.feature.home.MemoryGridAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrashActivity : AppCompatActivity() {
    @Inject lateinit var memoryRepository: MemoryRepository

    private lateinit var binding: ActivityTrashBinding
    private lateinit var adapter: MemoryGridAdapter
    private val selectedIds = linkedSetOf<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = MemoryGridAdapter(
            onMemoryClick = {
                if (selectedIds.isNotEmpty()) {
                    toggleSelection(it.id)
                }
            },
            onActionClick = { confirmPermanentDelete(it.id) },
            onMemoryLongClick = {
                toggleSelection(it.id)
                true
            },
            actionMode = MemoryGridAdapter.CardActionMode.DELETE,
        )
        binding.trashRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.trashRecyclerView.adapter = adapter
        setSupportActionBar(binding.trashToolbar)
        binding.trashToolbar.setNavigationOnClickListener { finish() }
        binding.restoreSelectedButton.setOnClickListener { restoreSelection() }
        binding.permanentDeleteSelectedButton.setOnClickListener { confirmPermanentDeleteSelection() }
        binding.clearTrashSelectionButton.setOnClickListener { clearSelection() }
        lifecycleScope.launch {
            memoryRepository.memories.collect { render() }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_trash, menu)
        menu?.findItem(R.id.action_empty_trash)?.tintIcon()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_empty_trash -> {
                confirmEmptyTrash()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun render() {
        val items = memoryRepository.trashedMemories()
        selectedIds.retainAll(items.map { it.id }.toSet())
        adapter.submitList(items)
        updateSelectionUi()
        binding.trashEmptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun toggleSelection(memoryId: Long) {
        if (!selectedIds.add(memoryId)) {
            selectedIds.remove(memoryId)
        }
        updateSelectionUi()
    }

    private fun clearSelection() {
        selectedIds.clear()
        updateSelectionUi()
    }

    private fun updateSelectionUi() {
        val count = selectedIds.size
        adapter.setSelectedIds(selectedIds)
        binding.trashSelectionBar.visibility = if (count > 0) View.VISIBLE else View.GONE
        binding.trashSelectedCountText.text = "${count}개 선택"
        binding.restoreSelectedButton.isEnabled = count > 0
        binding.permanentDeleteSelectedButton.isEnabled = count > 0
    }

    private fun restoreSelection() {
        val ids = selectedIds.toList()
        if (ids.isEmpty()) return
        ids.forEach { memoryRepository.restore(it) }
        clearSelection()
        Toast.makeText(this, "${ids.size}개 항목을 복원했어요.", Toast.LENGTH_SHORT).show()
    }

    private fun confirmPermanentDeleteSelection() {
        val ids = selectedIds.toList()
        if (ids.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("영구 삭제")
            .setMessage("선택한 ${ids.size}개 항목을 영구 삭제할까요? 복구할 수 없어요.")
            .setNegativeButton("취소", null)
            .setPositiveButton("영구 삭제") { _, _ -> permanentDelete(ids) }
            .show()
    }

    private fun confirmPermanentDelete(memoryId: Long) {
        AlertDialog.Builder(this)
            .setTitle("영구 삭제")
            .setMessage("이 항목을 영구 삭제할까요? 복구할 수 없어요.")
            .setNegativeButton("취소", null)
            .setPositiveButton("영구 삭제") { _, _ -> permanentDelete(listOf(memoryId)) }
            .show()
    }

    private fun confirmEmptyTrash() {
        val trashed = memoryRepository.trashedMemories()
        if (trashed.isEmpty()) {
            Toast.makeText(this, "휴지통이 비어 있어요.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("휴지통 비우기")
            .setMessage("휴지통의 ${trashed.size}개 항목을 영구 삭제할까요? 복구할 수 없어요.")
            .setNegativeButton("취소", null)
            .setPositiveButton("영구 삭제") { _, _ -> permanentDelete(trashed.map { it.id }) }
            .show()
    }

    private fun permanentDelete(ids: List<Long>) {
        lifecycleScope.launch {
            var failures = 0
            ids.forEach { id ->
                val result = memoryRepository.permanentDelete(id)
                if (result is AppResult.Error) failures++
            }
            val message = when {
                failures == 0 -> "${ids.size}개 항목을 영구 삭제했어요."
                failures == ids.size -> "삭제에 실패했어요."
                else -> "${ids.size - failures}개 삭제, ${failures}개 실패."
            }
            Toast.makeText(this@TrashActivity, message, Toast.LENGTH_SHORT).show()
            clearSelection()
        }
    }

    private fun MenuItem.tintIcon() {
        val source = icon ?: return
        val wrapped = DrawableCompat.wrap(source).mutate()
        DrawableCompat.setTint(wrapped, ContextCompat.getColor(this@TrashActivity, R.color.snap_text))
        icon = wrapped
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (selectedIds.isNotEmpty()) {
            clearSelection()
        } else {
            super.onBackPressed()
        }
    }
}
