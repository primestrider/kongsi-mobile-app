package com.primestrider.kotlinstarter.feature.example

import kotlinx.serialization.Serializable

// Routes of the account feature; the `feature.example` package keeps their serial names stable.

@Serializable
data object SignInRoute

@Serializable
data object ProfileRoute

/** Protected: only reachable with a session; see the guard in `accountDestinations`. */
@Serializable
data object AccountRoute
