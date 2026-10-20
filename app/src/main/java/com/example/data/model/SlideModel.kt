package com.example.data.model

data class SlideModel(
    val slideNumber: Int,
    val title: String,
    val subtitle: String = "",
    val bulletPoints: List<String> = emptyList(),
    val rawText: String = "",
    val speakerNotes: String = "",
    val category: String = "Slide",
    val accentColorHex: Long = 0xFF0284C7
)
