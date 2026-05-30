package com.example.snapmind.feature.search

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.example.snapmind.databinding.ActivitySearchBinding
import com.example.snapmind.feature.home.MemoryGridAdapter
import com.example.snapmind.feature.memorydetail.DetailActivity
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: MemoryGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MemoryGridAdapter(
            onMemoryClick = { startActivity(DetailActivity.createIntent(this, it.id)) },
            onFavoriteClick = { viewModel.toggleFavorite(it.id) },
        )
        binding.searchRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.searchRecyclerView.adapter = adapter
        binding.searchToolbar.setNavigationOnClickListener { finish() }

        binding.searchEditText.doOnTextChanged { text, _, _, _ ->
            val current = text?.toString().orEmpty()
            if (current != viewModel.query.value) viewModel.onQueryChanged(current)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderFilterChips(state)
                    adapter.submitList(state.results)
                    binding.searchEmptyState.visibility = if (state.isEmpty) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun renderFilterChips(state: SearchUiState) {
        with(binding.searchFilterChips) {
            removeAllViews()
            addView(
                Chip(this@SearchActivity).apply {
                    text = "전체"
                    isCheckable = true
                    isChecked = state.tagName == null && state.category == null
                    setOnClickListener { viewModel.clearFilters() }
                },
            )
            state.tags.forEach { tag ->
                addView(
                    Chip(this@SearchActivity).apply {
                        text = tag.displayName
                        isCheckable = true
                        isChecked = state.tagName?.equals(tag.name, ignoreCase = true) == true
                        setOnClickListener { viewModel.onTagSelected(tag.name) }
                    },
                )
            }
            state.categories.forEach { category ->
                addView(
                    Chip(this@SearchActivity).apply {
                        text = category.category.displayName
                        isCheckable = true
                        isChecked = state.category == category.category
                        setOnClickListener { viewModel.onCategorySelected(category.category) }
                    },
                )
            }
        }
    }
}
