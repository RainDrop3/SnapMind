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
            onFavoriteClick = { item -> viewModel.toggleFavorite(item.id) },
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
        binding.tagChipGroup.removeAllViews()
        binding.tagChipGroup.addView(
            Chip(requireContext()).apply {
                text = "전체"
                isCheckable = true
                isChecked = state.selectedTag == null
                setOnClickListener { viewModel.clearFilters() }
            },
        )
        state.tags.forEach { tag ->
            binding.tagChipGroup.addView(
                Chip(requireContext()).apply {
                    text = "${tag.displayName} ${tag.count}"
                    isCheckable = true
                    isChecked = state.selectedTag?.equals(tag.name, ignoreCase = true) == true
                    setOnClickListener { viewModel.applyTagFilter(tag.name) }
                },
            )
        }
        adapter.submitList(state.tagItems)
        binding.tagEmptyState.visibility = if (state.tagItems.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        binding.tagMemoryRecyclerView.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
