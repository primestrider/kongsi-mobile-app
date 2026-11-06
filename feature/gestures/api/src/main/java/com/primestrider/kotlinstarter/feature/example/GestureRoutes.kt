package com.primestrider.kotlinstarter.feature.example

import kotlinx.serialization.Serializable

// Routes of the gestures feature; the `feature.example` package keeps their serial names stable.

/** The six pages of the "Gestures" Home section; see `gesturePages`. */
@Serializable
data object GestureTapRoute

@Serializable
data object GestureDragRoute

@Serializable
data object GestureTransformRoute

@Serializable
data object GestureSwipeRoute

@Serializable
data object GestureScrollRoute

@Serializable
data object GesturePointerRoute
