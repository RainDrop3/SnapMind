package com.example.snapmind.feature.utility

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snapmind.data.model.TagCount
import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.databinding.ActivityTagManagementBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TagManagementActivity : AppCompatActivity() {
    @Inject lateinit var memoryRepository: MemoryRepository

    private lateinit var binding: ActivityTagManagementBinding
    private lateinit var adapter: TagManageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = TagManageAdapter(onDelete = { confirmDelete(it) })
        binding.tagRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.tagRecyclerView.adapter = adapter

        binding.tagToolbar.setNavigationOnClickListener { finish() }
        binding.addTagButton.setOnClickListener { addTag() }

        reload()
    }

    private fun reload() {
        lifecycleScope.launch {
            val tags = memoryRepository.listAllTags()
            adapter.submitList(tags)
            binding.tagRecyclerView.visibility = if (tags.isEmpty()) View.GONE else View.VISIBLE
            binding.tagEmptyState.visibility = if (tags.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun addTag() {
        val name = binding.newTagEditText.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            Toast.makeText(this, "태그 이름을 입력하세요.", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            val created = memoryRepository.createTag(name)
            if (created) {
                binding.newTagEditText.setText("")
                reload()
            } else {
                Toast.makeText(this@TagManagementActivity, "이미 있는 태그예요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun confirmDelete(tag: TagCount) {
        val message = if (tag.count > 0) {
            "'${tag.displayName}' 태그를 삭제할까요? ${tag.count}개 이미지에서 함께 제거됩니다."
        } else {
            "'${tag.displayName}' 태그를 삭제할까요?"
        }
        AlertDialog.Builder(this)
            .setTitle("태그 삭제")
            .setMessage(message)
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                lifecycleScope.launch {
                    memoryRepository.deleteTag(tag.name)
                    reload()
                }
            }
            .show()
    }
}
