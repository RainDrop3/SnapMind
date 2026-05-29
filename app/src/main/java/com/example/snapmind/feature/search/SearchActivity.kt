package com.example.snapmind.feature.search

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.databinding.ActivitySearchBinding
import com.example.snapmind.feature.home.MemoryGridAdapter
import com.example.snapmind.feature.memorydetail.DetailActivity
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {
    @Inject lateinit var memoryRepository: MemoryRepository

    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: MemoryGridAdapter
    private var selectedTag: String? = null
    private var selectedCategory: MemoryCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = MemoryGridAdapter(
            onMemoryClick = { startActivity(DetailActivity.createIntent(this, it.id)) },
            onFavoriteClick = {
                memoryRepository.toggleFavorite(it.id)
                renderResults()
            },
        )
        binding.searchRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.searchRecyclerView.adapter = adapter
        binding.searchToolbar.setNavigationOnClickListener { finish() }
        binding.searchEditText.doOnTextChanged { _, _, _, _ -> renderResults() }

        renderFilterChips()
        lifecycleScope.launch {
            memoryRepository.memories.collect {
                renderFilterChips()
                renderResults()
            }
        }
    }

    private fun renderFilterChips() {
        with(binding.searchFilterChips) {
            removeAllViews()
            addView(
                Chip(this@SearchActivity).apply {
                    text = "전체"
                    isCheckable = true
                    isChecked = selectedTag == null && selectedCategory == null
                    setOnClickListener {
                        selectedTag = null
                        selectedCategory = null
                        renderFilterChips()
                        renderResults()
                    }
                },
            )
            memoryRepository.tags().forEach { tag ->
                addView(
                    Chip(this@SearchActivity).apply {
                        text = tag.displayName
                        isCheckable = true
                        isChecked = selectedTag?.equals(tag.name, ignoreCase = true) == true
                        setOnClickListener {
                            selectedTag = tag.name
                            selectedCategory = null
                            renderFilterChips()
                            renderResults()
                        }
                    },
                )
            }
            memoryRepository.categoryCounts().forEach { category ->
                addView(
                    Chip(this@SearchActivity).apply {
                        text = category.category.displayName
                        isCheckable = true
                        isChecked = selectedCategory == category.category
                        setOnClickListener {
                            selectedCategory = category.category
                            selectedTag = null
                            renderFilterChips()
                            renderResults()
                        }
                    },
                )
            }
        }
    }

    private fun renderResults() {
        val results = memoryRepository.searchMemories(
            query = binding.searchEditText.text?.toString().orEmpty(),
            tagName = selectedTag,
            category = selectedCategory,
        )
        adapter.submitList(results)
        binding.searchEmptyState.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
    }
}
