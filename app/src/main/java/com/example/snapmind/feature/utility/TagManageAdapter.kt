package com.example.snapmind.feature.utility

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.snapmind.data.model.TagCount
import com.example.snapmind.databinding.ItemTagManageBinding

class TagManageAdapter(
    private val onDelete: (TagCount) -> Unit,
) : ListAdapter<TagCount, TagManageAdapter.TagViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ItemTagManageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return TagViewHolder(binding, onDelete)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TagViewHolder(
        private val binding: ItemTagManageBinding,
        private val onDelete: (TagCount) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TagCount) = with(binding) {
            tagName.text = item.displayName
            tagCount.text = "${item.count}개"
            tagDeleteButton.setOnClickListener { onDelete(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<TagCount>() {
        override fun areItemsTheSame(oldItem: TagCount, newItem: TagCount): Boolean =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: TagCount, newItem: TagCount): Boolean =
            oldItem == newItem
    }
}
