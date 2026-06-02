package com.example.snapmind.feature.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.snapmind.R
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.databinding.FragmentMemoryGridBinding
import com.example.snapmind.feature.memorydetail.DetailActivity
import com.example.snapmind.ui.main.MainUiState
import com.example.snapmind.ui.main.MainViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
abstract class MemoryGridFragment : Fragment(R.layout.fragment_memory_grid) {
    private var _binding: FragmentMemoryGridBinding? = null
    private val binding get() = checkNotNull(_binding)
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: MemoryGridAdapter
    private val selectedIds = linkedSetOf<Long>()
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            clearSelection()
        }
    }

    abstract val emptyTitle: String
    abstract val emptyMessage: String
    abstract fun selectItems(state: MainUiState): List<MemoryItem>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMemoryGridBinding.bind(view)
        adapter = MemoryGridAdapter(
            onMemoryClick = { item ->
                if (selectedIds.isNotEmpty()) {
                    toggleSelection(item.id)
                } else {
                    startActivity(DetailActivity.createIntent(requireContext(), item.id))
                }
            },
            onActionClick = { item -> viewModel.toggleFavorite(item.id) },
            onMemoryLongClick = { item ->
                toggleSelection(item.id)
                true
            },
        )
        binding.memoryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.memoryRecyclerView.adapter = adapter
        binding.emptyTitle.text = emptyTitle
        binding.emptyMessage.text = emptyMessage
        binding.deleteSelectedButton.setOnClickListener { confirmDeleteSelection() }
        binding.exportSelectedPdfButton.setOnClickListener { exportSelectionToPdf() }
        binding.clearSelectionButton.setOnClickListener { clearSelection() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: MainUiState) {
        val items = selectItems(state)
        selectedIds.retainAll(items.map { it.id }.toSet())
        adapter.submitList(items)
        updateSelectionUi()
        binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        renderFilterChip(state.activeFilterLabel)
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
        binding.selectionBar.visibility = if (count > 0) View.VISIBLE else View.GONE
        binding.selectedCountText.text = "${count}개 선택"
        binding.deleteSelectedButton.isEnabled = count > 0
        binding.exportSelectedPdfButton.isEnabled = count > 0
        backCallback.isEnabled = count > 0
    }

    private fun confirmDeleteSelection() {
        val ids = selectedIds.toList()
        if (ids.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle("선택 항목 삭제")
            .setMessage("${ids.size}개 항목을 휴지통으로 이동할까요?")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                viewModel.softDelete(ids)
                clearSelection()
                Toast.makeText(requireContext(), "${ids.size}개 항목을 휴지통으로 이동했어요.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun exportSelectionToPdf() {
        val ids = selectedIds.toList()
        if (ids.isEmpty()) return
        binding.exportSelectedPdfButton.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = viewModel.exportToPdf(ids)) {
                is AppResult.Success -> {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, result.data)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(share, "SnapMind PDF 공유"))
                    clearSelection()
                }
                is AppResult.Error -> {
                    Toast.makeText(
                        requireContext(),
                        "PDF 생성에 실패했어요.",
                        Toast.LENGTH_SHORT,
                    ).show()
                    updateSelectionUi()
                }
            }
        }
    }

    private fun renderFilterChip(label: String?) = with(binding.filterChipGroup) {
        removeAllViews()
        if (label == null) {
            visibility = View.GONE
            return@with
        }
        visibility = View.VISIBLE
        addView(
            Chip(requireContext()).apply {
                text = "필터: $label"
                isCloseIconVisible = true
                setOnCloseIconClickListener { viewModel.clearFilters() }
            },
        )
    }

    override fun onDestroyView() {
        binding.memoryRecyclerView.adapter = null
        selectedIds.clear()
        _binding = null
        super.onDestroyView()
    }
}
