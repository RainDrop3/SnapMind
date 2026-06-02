package com.example.snapmind.feature.tagbrowse

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.snapmind.R
import com.example.snapmind.databinding.FragmentTagBrowseBinding
import com.example.snapmind.feature.memorydetail.DetailActivity
import com.example.snapmind.feature.home.MemoryGridAdapter
import com.example.snapmind.ui.main.MainUiState
import com.example.snapmind.ui.main.MainViewModel
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TagBrowseFragment : Fragment(R.layout.fragment_tag_browse) {
    private var _binding: FragmentTagBrowseBinding? = null
    private val binding get() = checkNotNull(_binding)
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: MemoryGridAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentTagBrowseBinding.bind(view)
        adapter = MemoryGridAdapter(
            onMemoryClick = { item -> startActivity(DetailActivity.createIntent(requireContext(), item.id)) },
            onActionClick = { item -> viewModel.toggleFavorite(item.id) },
        )
        binding.tagMemoryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.tagMemoryRecyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: MainUiState) {
        val noFilter = state.tagBrowseSelectedCategory == null && state.tagBrowseSelectedTag == null

        // 카테고리 행 (위) — '전체' + 모델 분류 카테고리들.
        binding.categoryChipGroup.removeAllViews()
        binding.categoryChipGroup.addChip(text = "전체", selected = noFilter) {
            viewModel.clearTagBrowseFilters()
        }
        state.categories.forEach { categoryCount ->
            val selected = state.tagBrowseSelectedCategory == categoryCount.category
            binding.categoryChipGroup.addChip(
                text = "${categoryCount.category.displayName} ${categoryCount.count}",
                selected = selected,
            ) {
                if (selected) viewModel.clearTagBrowseFilters() else viewModel.applyTagBrowseCategory(categoryCount.category)
            }
        }

        // 태그 행 (아래) — 사용자가 추가한 태그들.
        binding.tagChipGroup.removeAllViews()
        state.tags.forEach { tag ->
            val selected = state.tagBrowseSelectedTag?.equals(tag.name, ignoreCase = true) == true
            binding.tagChipGroup.addChip(
                text = "${tag.displayName} ${tag.count}",
                selected = selected,
            ) {
                // 카테고리·태그는 동시 선택 불가. 같은 칩을 다시 누르면 필터 해제.
                if (selected) viewModel.clearTagBrowseFilters() else viewModel.applyTagBrowseTag(tag.name)
            }
        }

        adapter.submitList(state.tagBrowseItems)
        binding.tagEmptyState.visibility = if (state.tagBrowseItems.isEmpty()) View.VISIBLE else View.GONE
    }

    private inline fun com.google.android.material.chip.ChipGroup.addChip(
        text: String,
        selected: Boolean,
        crossinline onClick: () -> Unit,
    ) {
        addView(
            Chip(context).apply {
                this.text = text
                isCheckable = true
                isChecked = selected
                setOnClickListener { onClick() }
            },
        )
    }

    override fun onDestroyView() {
        binding.tagMemoryRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
