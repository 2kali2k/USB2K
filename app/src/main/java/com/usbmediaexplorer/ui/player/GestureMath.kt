package com.usbmediaexplorer.ui.player

internal fun scaledGestureValue(
    baseValue: Float,
    dragPixels: Float,
    extentPixels: Float,
    sensitivity: Float,
    thresholdPixels: Float,
): Float? {
    if (kotlin.math.abs(dragPixels) < thresholdPixels) return null
    return (baseValue - dragPixels / extentPixels.coerceAtLeast(1f) * sensitivity)
        .coerceIn(0f, 1f)
}

internal fun seekPositionFromDrag(
    startPositionMs: Long,
    dragPixels: Float,
    widthPixels: Float,
    durationMs: Long,
    sensitivity: Float,
): Long = (startPositionMs + dragPixels / widthPixels.coerceAtLeast(1f) * durationMs * sensitivity)
    .coerceIn(0L, durationMs)
    .toLong()