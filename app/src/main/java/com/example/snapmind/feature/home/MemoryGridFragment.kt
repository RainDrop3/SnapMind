package com.example.snapmind.feature.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.snapmind.R
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

    abstract val emptyTitle: String
    abstract val emptyMessage: String
    abstract fun selectItems(state: MainUiState): List<MemoryItem>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMemoryGridBinding.bind(view)
        adapter = MemoryGridAdapter(
            onMemoryClick = { item -> startActivity(DetailActivity.createIntent(requireContext(), item.id)) },
            onFavoriteClick = { item -> viewModel.toggleFavorite(item.id) },
        )
        binding.memoryRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.memoryRecyclerView.adapter = adapter
        binding.emptyTitle.text = emptyTitle
        binding.emptyMessage.text = emptyMessage

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: MainUiState) {
        val items = selectItems(state)
        adapter.submitList(items)
        binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        renderFilterChip(state.activeFilterLabel)
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
        _binding = null
        super.onDestroyView()
    }
}
