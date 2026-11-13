package com.tk.quickcontacts.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

/**
 * Material 3 Expressive motion tokens. Spatial springs (position, size, rotation)
 * carry a little bounce; effects springs (alpha, color) never overshoot.
 */
object ExpressiveMotion {
    // Spatial
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)
    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)

    /** Same speed as [defaultSpatial] but critically damped, for container size changes. */
    fun <T> defaultSpatialNoBounce(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 380f)

    // Effects
    fun <T> fastEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 3800f)
    fun <T> defaultEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
    fun <T> slowEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 800f)
}
