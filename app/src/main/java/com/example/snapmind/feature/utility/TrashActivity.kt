package com.example.snapmind.feature.utility

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = MemoryGridAdapter(
            onMemoryClick = {
                memoryRepository.restore(it.id)
                Toast.makeText(this, "복구했어요.", Toast.LENGTH_SHORT).show()
            },
            onFavoriteClick = { memoryRepository.restore(it.id) },
        )
        binding.trashRecyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.trashRecyclerView.adapter = adapter
        binding.trashToolbar.setNavigationOnClickListener { finish() }
        lifecycleScope.launch {
            memoryRepository.memories.collect { render() }
        }
    }

    private fun render() {
        val items = memoryRepository.trashedMemories()
        adapter.submitList(items)
        binding.trashEmptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}
