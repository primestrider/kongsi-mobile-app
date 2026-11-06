package com.primestrider.kotlinstarter.core.network

import okhttp3.Request
import retrofit2.Invocation

/** Marks a Retrofit method whose request must not carry the bearer token. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anonymous

/** True when the Retrofit method behind this request is annotated [Anonymous]. */
fun Request.isAnonymous(): Boolean = tag(Invocation::class.java)?.method()?.isAnnotationPresent(Anonymous::class.java) == true
