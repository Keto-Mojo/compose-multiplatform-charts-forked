package com.netguru.multiplatform.charts.line

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.netguru.multiplatform.charts.ChartAnimation
import com.netguru.multiplatform.charts.getAnimationAlphas
import com.netguru.multiplatform.charts.grid.*
import com.netguru.multiplatform.charts.grid.axisscale.x.TimestampXAxisScale
import com.netguru.multiplatform.charts.grid.axisscale.y.YAxisScaleDynamic
import com.netguru.multiplatform.charts.grid.axisscale.y.YAxisScaleStatic
import com.netguru.multiplatform.charts.theme.ChartColors
import com.netguru.multiplatform.charts.theme.ChartTheme

/**
 * Classic line chart with some shade below the line in the same color (albeit with a lot of
 * transparency) as the line and floating balloon on touch/click to show values for that particular
 * x-axis value.
 *
 * Color, shape and whether the line is dashed for each of the lines is specified in the
 * [LegendItemData] class.
 *
 * If the [lineChartDataRightAxis] param is null, ordinary [LineChart] is used with the data and
 * settings for the left Y-axis.
 *
 * @param lineChartDataLeftAxis Data to portray on the left Y axis
 * @param lineChartDataRightAxis Data to portray on the right Y axis
 * @param colors Colors used are [ChartColors.grid], [ChartColors.surface] and
 * [ChartColors.overlayLine].
 * @param xAxisLabel Composable to mark the values on the x-axis.
 * @param leftYAxisMarkerLayout Composable to mark the values on the left y-axis.
 * @param rightYAxisMarkerLayout Composable to mark the values on the right y-axis.
 * @param overlayHeaderLabel Composable to show the current x-axis value on the overlay balloon
 * @param overlayDataEntryLabel Composable to show the value of each line in the overlay balloon
 * for that specific x-axis value
 * @param animation Animation to use
 * @param maxVerticalLines Max number of lines, representing the x-axis values
 * @param maxHorizontalLines Max number of lines, representing the y-axis values
 * @param roundLeftMinMaxClosestTo Number to which min and max range on the left will be rounded to
 * @param roundRightMinMaxClosestTo Number to which min and max range on the right will be rounded to
 */
@Composable
fun LineChartWithTwoYAxisSets(
    leftYAxisData: YAxisData,
    rightYAxisData: YAxisData?,
    modifier: Modifier = Modifier,
    colors: LineChartColors = ChartTheme.colors.lineChartColors,
    overlayData: OverlayData? = OverlayData(),
    xAxisData: XAxisData? = XAxisData(),
    animation: ChartAnimation = ChartAnimation.Simple(),
    maxVerticalLines: Int = GridDefaults.NUMBER_OF_GRID_LINES,
    verticalPathEffect: PathEffect? = null,
    maxHorizontalLines: Int = GridDefaults.NUMBER_OF_GRID_LINES,
    horizontalPathEffect: PathEffect? = null,
    drawPoints: Boolean = false,
    legendData: LegendData? = LegendData()
) {
    if (rightYAxisData != null) {
        LineChartWithTwoYAxisSetsLayout(
            leftYAxisData = leftYAxisData,
            rightYAxisData = rightYAxisData,
            modifier = modifier,
            colors = colors,
            xAxisData = xAxisData,
            overlayData = overlayData,
            animation = animation,
            maxVerticalLines = maxVerticalLines,
            verticalPathEffect = verticalPathEffect,
            maxHorizontalLines = maxHorizontalLines,
            horizontalPathEffect = horizontalPathEffect,
            drawPoints = drawPoints,
            legendData = legendData,
        )
    } else {
        LineChart(
            yAxisData = leftYAxisData,
            modifier = modifier,
            colors = colors,
            xAxisData = xAxisData,
            overlayData = overlayData,
            animation = animation,
            maxVerticalLines = maxVerticalLines,
            verticalPathEffect = verticalPathEffect,
            maxHorizontalLines = maxHorizontalLines,
            horizontalPathEffect = horizontalPathEffect,
            drawPoints = drawPoints,
            legendData = legendData,
        )
    }
}

@Composable
private fun LineChartWithTwoYAxisSetsLayout(
    leftYAxisData: YAxisData,
    rightYAxisData: YAxisData,
    modifier: Modifier,
    colors: LineChartColors,
    xAxisData: XAxisData?,
    overlayData: OverlayData?,
    animation: ChartAnimation,
    maxVerticalLines: Int,
    verticalPathEffect: PathEffect?,
    maxHorizontalLines: Int,
    horizontalPathEffect: PathEffect?,
    drawPoints: Boolean,
    legendData: LegendData?,
) {
    var touchPositionX by remember { mutableStateOf(-1f) }
    var verticalGridLines by remember { mutableStateOf(emptyList<LineParameters>()) }
    var leftYAxisMarks by remember { mutableStateOf(emptyList<LineParameters>()) }
    var rightYAxisMarks by remember { mutableStateOf(emptyList<LineParameters>()) }
    val horizontalLinesOffset: Dp = GridDefaults.HORIZONTAL_LINES_OFFSET

    val alphas = getAnimationAlphas(
        animation = animation,
        numberOfElementsToAnimate = leftYAxisData.lineChartData.series.size + rightYAxisData.lineChartData.series.size,
        uniqueDatasetKey = LineChartData(
            series = leftYAxisData.lineChartData.series + rightYAxisData.lineChartData.series,
            dataUnit = null,
        ),
    )

    fun LineChartData.addNoYValuePointsFrom(another: LineChartData): LineChartData {
        val anotherSeries = another.series
            .map { it.copy(listOfPoints = it.listOfPoints.map { point -> point.copy(y = null) }) }

        return copy(series = series + anotherSeries)
    }

    Column(
        modifier = modifier,
    ) {
        if (leftYAxisData.yAxisTitleData?.labelPosition == YAxisTitleData.LabelPosition.Top ||
            rightYAxisData.yAxisTitleData?.labelPosition == YAxisTitleData.LabelPosition.Top
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                leftYAxisData.yAxisTitleData?.labelLayout?.invoke() ?: Spacer(Modifier.size(1.dp))
                rightYAxisData.yAxisTitleData?.labelLayout?.invoke() ?: Spacer(Modifier.size(1.dp))
            }
        }
        Row(modifier = Modifier.weight(1f)) {
            if (leftYAxisData.markerLayout != null) {
                YAxisLabels(
                    horizontalGridLines = leftYAxisMarks,
                    yAxisMarkerLayout = leftYAxisData.markerLayout,
                    yAxisTitleData = leftYAxisData.yAxisTitleData,
                    modifier = Modifier
                        .padding(end = 8.dp)
                )
            }

            val numberOfXAxisEntries by remember(leftYAxisData.lineChartData, rightYAxisData.lineChartData) {
                derivedStateOf {
                    (leftYAxisData.lineChartData.series +
                            rightYAxisData.lineChartData.series
                            )
                        .map {
                            it.listOfPoints
                        }
                        .maxOf {
                            it.size
                        }
                }
            }

            // main chart
            Column(Modifier.weight(1f)) {
                var pointsToDraw: List<SeriesAndClosestPoint> by remember {
                    mutableStateOf(emptyList())
                }
                val xAxisScale = TimestampXAxisScale(
                    min = minOf(leftYAxisData.lineChartData.minX, rightYAxisData.lineChartData.minX),
                    max = maxOf(leftYAxisData.lineChartData.maxX, rightYAxisData.lineChartData.maxX),
                    maxTicksCount = (minOf(
                        maxVerticalLines, numberOfXAxisEntries
                    ) - 1).coerceAtLeast(1),
                    roundClosestTo = xAxisData?.roundMinMaxClosestTo
                )
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .drawBehind {
                            val lines = measureChartGrid(
                                xAxisScale = xAxisScale,
                                yAxisScale = YAxisScaleStatic(
                                    min = 0f,
                                    max = maxHorizontalLines.toFloat(),
                                    maxTickCount = maxHorizontalLines - 1,
                                    roundClosestTo = 1f,
                                ),
                            ).also {
                                verticalGridLines = it.verticalLines
                            }

                            leftYAxisMarks = measureChartGrid(
                                xAxisScale = TimestampXAxisScale(
                                    min = 0,
                                    max = 0,
                                    roundClosestTo = xAxisData?.roundMinMaxClosestTo,
                                ),
                                yAxisScale = YAxisScaleDynamic(
                                    min = leftYAxisData.lineChartData.minY,
                                    max = leftYAxisData.lineChartData.maxY,
                                    maxTickCount = maxHorizontalLines - 1,
                                    roundClosestTo = leftYAxisData.roundMinMaxClosestTo,
                                )
                            )
                                .horizontalLines
                                .let {
                                    val containsZeroValue =
                                        it.firstOrNull { line -> line.position == lines.zeroPosition.position } != null
                                    if (containsZeroValue) {
                                        it
                                    } else {
                                        it + lines.zeroPosition
                                    }
                                }
                            rightYAxisMarks = measureChartGrid(
                                xAxisScale = TimestampXAxisScale(
                                    min = 0,
                                    max = 0,
                                    roundClosestTo = xAxisData?.roundMinMaxClosestTo,
                                ),
                                yAxisScale = YAxisScaleDynamic(
                                    min = rightYAxisData.lineChartData.minY,
                                    max = rightYAxisData.lineChartData.maxY,
                                    maxTickCount = maxHorizontalLines - 1,
                                    roundClosestTo = rightYAxisData.roundMinMaxClosestTo,
                                )
                            )
                                .horizontalLines
                                .let {
                                    val containsZeroValue =
                                        it.firstOrNull { line -> line.position == lines.zeroPosition.position } != null
                                    if (containsZeroValue) {
                                        it
                                    } else {
                                        it + lines.zeroPosition
                                    }
                                }

                            drawChartGrid(
                                grid = lines,
                                color = colors.grid,
                                horizontalPathEffect = horizontalPathEffect,
                                verticalPathEffect = verticalPathEffect,
                            )

                            drawLineChart(
                                // we have to join those points so that the x-values align properly. Otherwise, in case when
                                // datasets would not start and end at the same x value, they would still be drawn from the
                                // same start and end point, making (at least) one of them drawn incorrectly
                                lineChartData = leftYAxisData.lineChartData.addNoYValuePointsFrom(rightYAxisData.lineChartData),
                                graphTopPadding = horizontalLinesOffset,
                                graphBottomPadding = horizontalLinesOffset,
                                alpha = alphas,
                                drawPoints = drawPoints,
                                selectedPointsForDrawing = pointsToDraw.filter {
                                    leftYAxisData.lineChartData.series.contains(
                                        it.lineChartSeries
                                    )
                                },
                                xAxisScale = xAxisScale,
                            )

                            drawLineChart(
                                // we have to join those points so that the x-values align properly. Otherwise, in case when
                                // datasets would not start and end at the same x value, they would still be drawn from the
                                // same start and end point, making (at least) one of them drawn incorrectly
                                lineChartData = rightYAxisData.lineChartData.addNoYValuePointsFrom(leftYAxisData.lineChartData),
                                graphTopPadding = horizontalLinesOffset,
                                graphBottomPadding = horizontalLinesOffset,
                                alpha = alphas,
                                drawPoints = drawPoints,
                                selectedPointsForDrawing = pointsToDraw.filter {
                                    rightYAxisData.lineChartData.series.contains(
                                        it.lineChartSeries
                                    )
                                },
                                xAxisScale = xAxisScale,
                            )
                        }
                        // Touch input
                        .pointerInput(Unit) {
                            while (true) {
                                awaitPointerEventScope {
                                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)

                                    touchPositionX = if (
                                        shouldIgnoreTouchInput(
                                            event = event,
                                            containerSize = size
                                        )
                                    ) {
                                        -1f
                                    } else {
                                        event.changes[0].position.x
                                    }

                                    event.changes.any {
                                        it.consume()
                                        true
                                    }
                                }
                            }
                        },
                    content = {
                        // Overlay
                        if (overlayData != null) {
                            LineChartOverlayInformation(
                                lineChartData = listOf(leftYAxisData.lineChartData, rightYAxisData.lineChartData),
                                positionX = touchPositionX,
                                containerSize = with(LocalDensity.current) {
                                    Size(
                                        maxWidth.toPx(),
                                        maxHeight.toPx()
                                    )
                                },
                                colors = colors,
                                drawPoints = {
                                    pointsToDraw = it
                                },
                                overlayData = overlayData,
                                xAxisScale = xAxisScale,
                            )
                        }
                    }
                )

                if (xAxisData != null) {
                    Box(Modifier.fillMaxWidth()) {
                        DrawXAxisMarkers(
                            lineParams = verticalGridLines,
                            xAxisData = xAxisData,
                            modifier = Modifier
                                .fillMaxWidth()
                        )
                    }
                }
            }

            if (rightYAxisData.markerLayout != null) {
                YAxisLabels(
                    horizontalGridLines = rightYAxisMarks,
                    yAxisMarkerLayout = rightYAxisData.markerLayout,
                    yAxisTitleData = rightYAxisData.yAxisTitleData,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .zIndex(-1f)
                )
            }
        }

        if (legendData != null) {
            ChartLegend(
                legendData = leftYAxisData.lineChartData.legendData + rightYAxisData.lineChartData.legendData,
                animation = animation,
                legendItemLabel = legendData.legendItemLabel,
                columnMinWidth = legendData.columnMinWidth,
            )
        }
    }
}

private fun shouldIgnoreTouchInput(event: PointerEvent, containerSize: IntSize): Boolean {
    if (event.changes.isEmpty() ||
        event.type != PointerEventType.Move
    ) {
        return true
    }
    if (event.changes[0].position.x < 0 ||
        event.changes[0].position.x > containerSize.width
    ) {
        return true
    }
    if (event.changes[0].position.y < 0 ||
        event.changes[0].position.y > containerSize.height
    ) {
        return true
    }
    return false
}
