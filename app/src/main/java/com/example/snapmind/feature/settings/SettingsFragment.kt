package com.example.snapmind.feature.settings

import android.os.Bundle
import android.view.View
import android.widget.Toast
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
        setSafe(binding.visionSwitch, flags.visionEnabled)
        setSafe(binding.geminiSwitch, flags.geminiEnabled)
        setSafe(binding.youtubeSwitch, flags.youtubeEnabled)

        binding.visionSwitch.setOnCheckedChangeListener { _, checked -> prefs.visionEnabled = checked }
        binding.geminiSwitch.setOnCheckedChangeListener { _, checked -> prefs.geminiEnabled = checked }
        binding.youtubeSwitch.setOnCheckedChangeListener { _, checked -> prefs.youtubeEnabled = checked }
    }

    private fun setSafe(switch: SwitchMaterial, checked: Boolean) {
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = checked
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
}
