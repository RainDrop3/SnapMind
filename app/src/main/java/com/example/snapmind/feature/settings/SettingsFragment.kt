package com.example.snapmind.feature.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.switchmaterial.SwitchMaterial
import com.example.snapmind.R
import com.example.snapmind.core.settings.AppPreferences
import com.example.snapmind.databinding.FragmentSettingsBinding
import com.example.snapmind.ui.main.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = checkNotNull(_binding)
    private val viewModel: MainViewModel by activityViewModels()

    @Inject lateinit var prefs: AppPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSettingsBinding.bind(view)

        bindSwitches()
        bindInfoButtons()

        binding.clearPdfCacheButton.setOnClickListener {
            val freed = prefs.clearPdfCache()
            val msg = if (freed > 0) "PDF 캐시 ${formatBytes(freed)} 정리했어요."
                      else "정리할 PDF 캐시가 없어요."
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.storageText.text =
                        "저장된 메모리 ${state.memories.size}개 · 인기 태그 ${state.topTags.size}개 · 로컬 우선 모드"
                }
            }
        }
    }

    private fun bindSwitches() {
        val flags = prefs.current()
        binding.apiHealthText.text =
            "아래 옵션에서 링크 카드, 악성 링크 경고, 화질 업그레이드, 메모 추천 기능을 필요에 따라 켜고 끌 수 있습니다."
        setSafe(binding.visionSwitch, flags.linkPreviewEnabled)
        setSafe(binding.youtubeSwitch, flags.youtubeEnabled)
        setSafe(binding.safeBrowsingSwitch, flags.safeBrowsingEnabled)
        setSafe(binding.imageEnhancementSwitch, flags.imageEnhancementEnabled)
        setSafe(binding.geminiSwitch, flags.geminiEnabled)
        updateLinkChildOptionsEnabled(flags.linkPreviewEnabled)

        binding.visionSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.linkPreviewEnabled = checked
            updateLinkChildOptionsEnabled(checked)
        }
        binding.youtubeSwitch.setOnCheckedChangeListener { _, checked -> prefs.youtubeEnabled = checked }
        binding.safeBrowsingSwitch.setOnCheckedChangeListener { _, checked -> prefs.safeBrowsingEnabled = checked }
        binding.imageEnhancementSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.imageEnhancementEnabled = checked
        }
        binding.geminiSwitch.setOnCheckedChangeListener { _, checked -> prefs.geminiEnabled = checked }
    }

    private fun bindInfoButtons() {
        binding.linkPreviewInfoButton.setOnClickListener {
            showFeatureInfo(
                title = "OCR URL 링크 카드",
                message = "OCR 결과에서 URL을 찾으면 페이지 제목, 설명, 대표 이미지를 가져와 상세 화면에 링크 카드로 표시합니다. 이 옵션을 끄면 YouTube 링크 보강과 악성 링크 경고도 실행되지 않습니다.",
            )
        }
        binding.youtubeInfoButton.setOnClickListener {
            showFeatureInfo(
                title = "YouTube 링크 보강",
                message = "OCR에서 찾은 YouTube 링크를 YouTube Data API로 확인하고, 영상 제목과 썸네일 정보를 링크 카드에 반영합니다.",
            )
        }
        binding.safeBrowsingInfoButton.setOnClickListener {
            showFeatureInfo(
                title = "악성 링크 경고",
                message = "OCR에서 찾은 URL을 Google Safe Browsing API로 검사해 악성코드, 피싱, 원치 않는 소프트웨어 위험이 감지되면 경고합니다. 오탐과 미탐이 있을 수 있습니다.",
            )
        }
        binding.imageEnhancementInfoButton.setOnClickListener {
            showFeatureInfo(
                title = "화질 업그레이드 API",
                message = "상세 화면에서 사용자가 동의한 경우에만 이미지를 Clipdrop API 서버로 업로드해 업스케일 결과를 받아옵니다.",
            )
        }
        binding.geminiInfoButton.setOnClickListener {
            showFeatureInfo(
                title = "Gemini 메모 추천",
                message = "이미지를 Gemini API로 보내 저장 이유나 메모 초안을 추천받습니다. 사용자가 수락하기 전에는 메모 본문을 바로 덮어쓰지 않습니다.",
            )
        }
    }

    private fun updateLinkChildOptionsEnabled(enabled: Boolean) {
        binding.linkChildOptionsContainer.alpha = if (enabled) 1f else DISABLED_CHILD_ALPHA
        binding.youtubeSwitch.isEnabled = enabled
        binding.safeBrowsingSwitch.isEnabled = enabled
    }

    private fun setSafe(switch: SwitchMaterial, checked: Boolean) {
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = checked
    }

    private fun showFeatureInfo(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024.0)
        else -> "${bytes}B"
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val DISABLED_CHILD_ALPHA = 0.45f
    }
}
