package com.example.snapmind.ui.main

import com.example.snapmind.data.model.CategoryCount
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.MemoryItem
import com.example.snapmind.data.model.TagCount

data class MainUiState(
    val memories: List<MemoryItem> = emptyList(),
    val homeItems: List<MemoryItem> = emptyList(),
    val favoriteItems: List<MemoryItem> = emptyList(),
    val tags: List<TagCount> = emptyList(),
    val topTags: List<TagCount> = emptyList(),
    val categories: List<CategoryCount> = emptyList(),
    // 홈 화면 전용 필터(주로 Drawer 카테고리에서 설정).
    val selectedTag: String? = null,
    val selectedCategory: MemoryCategory? = null,
    // '태그별' 페이지 전용 필터. 홈/즐겨찾기에는 영향을 주지 않는다.
    val tagBrowseItems: List<MemoryItem> = emptyList(),
    val tagBrowseSelectedTag: String? = null,
    val tagBrowseSelectedCategory: MemoryCategory? = null,
) {
    val activeFilterLabel: String?
        get() = selectedTag?.let { "#${it.removePrefix("#")}" } ?: selectedCategory?.displayName
}
