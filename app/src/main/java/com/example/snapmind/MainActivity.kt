package com.example.snapmind

import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.example.snapmind.data.model.CategoryCount
import com.example.snapmind.data.model.MemoryCategory
import com.example.snapmind.data.model.TagCount
import com.example.snapmind.core.result.AppResult
import com.example.snapmind.data.repository.MemoryRepository
import com.example.snapmind.databinding.ActivityMainBinding
import com.example.snapmind.ui.main.MainPagerAdapter
import com.example.snapmind.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var memoryRepository: MemoryRepository

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val galleryPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importPickedImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        setupToolbar()
        setupPager(savedInstanceState)
        setupDrawer()
        collectState()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SELECTED_PAGE, binding.mainPager.currentItem)
        super.onSaveInstanceState(outState)
    }

    private fun setupToolbar() = with(binding.toolbar) {
        navigationIcon?.let { icon ->
            DrawableCompat.setTint(icon, ContextCompat.getColor(this@MainActivity, R.color.snap_text))
        }
        setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_search -> {
                    Toast.makeText(this@MainActivity, "검색 화면은 다음 브랜치에서 연결됩니다.", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupPager(savedInstanceState: Bundle?) = with(binding) {
        mainPager.adapter = MainPagerAdapter(this@MainActivity)
        mainPager.offscreenPageLimit = MainPagerAdapter.PAGE_COUNT - 1
        mainPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomNavigation.selectedItemId = position.toNavItemId()
                toolbar.menu.findItem(R.id.action_search)?.isVisible = position == MainPagerAdapter.PAGE_HOME
            }
        })
        bottomNavigation.setOnItemSelectedListener { item ->
            val page = item.itemId.toPageIndex()
            if (mainPager.currentItem != page) {
                mainPager.setCurrentItem(page, true)
            }
            true
        }

        val restoredPage = savedInstanceState?.getInt(KEY_SELECTED_PAGE, MainPagerAdapter.PAGE_HOME)
            ?: MainPagerAdapter.PAGE_HOME
        mainPager.setCurrentItem(restoredPage.coerceIn(0, MainPagerAdapter.PAGE_COUNT - 1), false)
        bottomNavigation.selectedItemId = mainPager.currentItem.toNavItemId()

        uploadFab.setOnClickListener {
            galleryPicker.launch("image/*")
        }
    }

    private fun importPickedImage(uri: Uri) {
        lifecycleScope.launch {
            when (memoryRepository.importImage(uri, contentResolver.getType(uri), "갤러리")) {
                is AppResult.Success -> {
                    binding.mainPager.setCurrentItem(MainPagerAdapter.PAGE_HOME, true)
                    Toast.makeText(this@MainActivity, "이미지를 SnapMind에 추가했어요.", Toast.LENGTH_SHORT).show()
                }
                is AppResult.Error -> {
                    Toast.makeText(this@MainActivity, "이미지를 추가하지 못했어요.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupDrawer() = with(binding) {
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                mainPager.isUserInputEnabled = false
            }

            override fun onDrawerClosed(drawerView: View) {
                mainPager.isUserInputEnabled = true
            }
        })
        trashButton.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            Toast.makeText(this@MainActivity, "휴지통 화면은 UI 브랜치에서 연결됩니다.", Toast.LENGTH_SHORT).show()
        }
        pdfButton.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            Toast.makeText(this@MainActivity, "PDF 추출 흐름은 UI 브랜치에서 연결됩니다.", Toast.LENGTH_SHORT).show()
        }
        developerButton.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            Toast.makeText(this@MainActivity, "개발자 소개는 UI 브랜치에서 연결됩니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderTopTags(state.topTags)
                    renderCategories(state.categories)
                }
            }
        }
    }

    private fun renderTopTags(tags: List<TagCount>) = with(binding.topTagContainer) {
        removeAllViews()
        if (tags.isEmpty()) {
            addView(drawerRow("태그 없음", "이미지가 쌓이면 자동으로 표시됩니다.", false) {})
            return@with
        }
        tags.forEachIndexed { index, tag ->
            addView(
                drawerRow(
                    title = "${index + 1}. ${tag.displayName}",
                    detail = "${tag.count}개 이미지",
                    selected = false,
                ) {
                    viewModel.applyTagFilter(tag.name)
                    binding.mainPager.setCurrentItem(MainPagerAdapter.PAGE_TAGS, true)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                },
            )
        }
    }

    private fun renderCategories(categories: List<CategoryCount>) = with(binding.categoryContainer) {
        removeAllViews()
        categories.forEach { categoryCount ->
            addView(
                drawerRow(
                    title = categoryCount.category.displayName,
                    detail = "${categoryCount.count}개 이미지",
                    selected = false,
                ) {
                    viewModel.applyCategoryFilter(categoryCount.category)
                    binding.mainPager.setCurrentItem(MainPagerAdapter.PAGE_HOME, true)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                },
            )
        }
        addView(
            drawerRow("전체 보기", "필터 해제", false) {
                viewModel.clearFilters()
                binding.mainPager.setCurrentItem(MainPagerAdapter.PAGE_HOME, true)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            },
        )
    }

    private fun drawerRow(
        title: String,
        detail: String,
        selected: Boolean,
        onClick: () -> Unit,
    ): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(54)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            background = ContextCompat.getDrawable(
                this@MainActivity,
                if (selected) R.drawable.bg_badge_soft else android.R.color.transparent,
            )
        }
        container.addView(TextView(this).apply {
            text = title
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.snap_text))
            setTypeface(typeface, Typeface.BOLD)
            textSize = 14f
        })
        container.addView(TextView(this).apply {
            text = detail
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.snap_text_secondary))
            textSize = 12f
        })
        return container
    }

    private fun Int.toNavItemId(): Int =
        when (this) {
            MainPagerAdapter.PAGE_FAVORITES -> R.id.nav_favorites
            MainPagerAdapter.PAGE_TAGS -> R.id.nav_tags
            MainPagerAdapter.PAGE_SETTINGS -> R.id.nav_settings
            else -> R.id.nav_home
        }

    private fun Int.toPageIndex(): Int =
        when (this) {
            R.id.nav_favorites -> MainPagerAdapter.PAGE_FAVORITES
            R.id.nav_tags -> MainPagerAdapter.PAGE_TAGS
            R.id.nav_settings -> MainPagerAdapter.PAGE_SETTINGS
            else -> MainPagerAdapter.PAGE_HOME
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private companion object {
        const val KEY_SELECTED_PAGE = "selected_page"
    }
}
