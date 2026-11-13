package com.tk.quickcontacts.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Material 3 Expressive corner scale.
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Expressive "large increased" / "extra large increased" corners. */
val LargeIncreasedShape = RoundedCornerShape(20.dp)
val ExtraLargeIncreasedShape = RoundedCornerShape(32.dp)

/**
 * Segmented list grouping (Android 16 / M3 Expressive): items in a group sit
 * 2.dp apart, share tight inner corners, and only the outer edges of the
 * group get the large rounded corners.
 */
object SegmentedList {
    val OuterCorner: Dp = 24.dp
    val InnerCorner: Dp = 6.dp
    val Gap: Dp = 2.dp

    fun shape(index: Int, count: Int): Shape {
        val isFirst = index == 0
        val isLast = index == count - 1
        return RoundedCornerShape(
            topStart = if (isFirst) OuterCorner else InnerCorner,
            topEnd = if (isFirst) OuterCorner else InnerCorner,
            bottomStart = if (isLast) OuterCorner else InnerCorner,
            bottomEnd = if (isLast) OuterCorner else InnerCorner,
        )
    }
}
