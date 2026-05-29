package com.example.snapmind.feature.utility

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.snapmind.databinding.ActivityDeveloperInfoBinding

class DeveloperInfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeveloperInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeveloperInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.developerToolbar.setNavigationOnClickListener { finish() }
    }
}
