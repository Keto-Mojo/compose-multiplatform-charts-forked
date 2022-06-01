package com.netguru.application.screen

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.House
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netguru.application.SpacedColumn
import com.netguru.application.TitleText
import com.netguru.charts.ChartAnimation
import com.netguru.charts.bubble.Bubble
import com.netguru.charts.bubble.BubbleChart
import com.netguru.common.AppTheme

@Composable
fun BubbleChartScreen() {
    val bubbles = listOf(
        Bubble(
            name = "first",
            value = 1.2f,
            icon = Icons.Default.Album,
            color = AppTheme.colors.chart1
        ),
        Bubble(
            name = "second",
            value = 4.6f,
            icon = Icons.Default.House,
            color = AppTheme.colors.chart2
        ),
        Bubble(
            name = "third",
            value = 6.9f,
            icon = Icons.Default.Bed,
            color = AppTheme.colors.chart3
        ),
    )

    SpacedColumn {
        TitleText(text = "Bubble chart")
        BubbleChart(
            bubbles = bubbles,
            modifier = Modifier
                .size(300.dp),
            animation = ChartAnimation.Sequenced(),
        )
    }
}