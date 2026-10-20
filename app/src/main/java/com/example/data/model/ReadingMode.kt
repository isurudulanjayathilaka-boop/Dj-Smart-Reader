package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.PaperForest
import com.example.ui.theme.PaperForestSurface
import com.example.ui.theme.PaperForestText
import com.example.ui.theme.PaperNight
import com.example.ui.theme.PaperNightSurface
import com.example.ui.theme.PaperNightText
import com.example.ui.theme.PaperSepia
import com.example.ui.theme.PaperSepiaSurface
import com.example.ui.theme.PaperSepiaText
import com.example.ui.theme.PaperText
import com.example.ui.theme.PaperWhite

enum class ReadingMode(
    val title: String,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val textColor: Color,
    val iconDescription: String
) {
    LIGHT(
        title = "Clean Light",
        backgroundColor = Color(0xFFF1F5F9),
        surfaceColor = PaperWhite,
        textColor = PaperText,
        iconDescription = "Standard daylight high-contrast mode"
    ),
    SEPIA(
        title = "Eye Comfort Sepia",
        backgroundColor = PaperSepia,
        surfaceColor = PaperSepiaSurface,
        textColor = PaperSepiaText,
        iconDescription = "Warm paper hue to reduce digital eye strain"
    ),
    NIGHT(
        title = "Dark OLED Night",
        backgroundColor = PaperNight,
        surfaceColor = PaperNightSurface,
        textColor = PaperNightText,
        iconDescription = "Dark mode for low-light environments"
    ),
    FOREST(
        title = "Forest Calm",
        backgroundColor = PaperForest,
        surfaceColor = PaperForestSurface,
        textColor = PaperForestText,
        iconDescription = "Soft gentle green tone for long reading sessions"
    )
}
