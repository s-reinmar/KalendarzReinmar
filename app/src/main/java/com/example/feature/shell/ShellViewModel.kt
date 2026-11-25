package com.example.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ShellViewModel(
    val controller: ShellController,
) : ViewModel() {
    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ShellViewModel(ShellController()) as T
        }
    }
}
