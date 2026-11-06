package com.primestrider.kotlinstarter.testing

import androidx.compose.ui.platform.AndroidUiDispatcher
import kotlin.coroutines.ContinuationInterceptor

/**
 * Un-sticks Compose's process-wide `AndroidUiDispatcher.Main` between Robolectric tests.
 *
 * The dispatcher posts one Handler message per batch and sets a "scheduled" flag until it runs. Robolectric clears
 * the main looper's queue after each test, so a batch scheduled at the end of one test (an IME action is enough)
 * leaves the flag set forever and every later dispatch is queued but never run. Paging's `LazyPagingItems` delivers
 * pages on that dispatcher (it cannot be swapped), so a paging screen test after such a test sees no data.
 * Call this in `@Before`: it drops the dead batch (work of a previous test's disposed composition) and clears the flags.
 *
 * Reflection on private fields: if a Compose update renames them this throws, pointing here.
 */
fun resetAndroidUiDispatcher() {
    val dispatcher = AndroidUiDispatcher.Main[ContinuationInterceptor] as AndroidUiDispatcher
    val type = AndroidUiDispatcher::class.java

    fun field(name: String) = type.getDeclaredField(name).apply { isAccessible = true }
    synchronized(field("lock").get(dispatcher)!!) {
        for (queue in listOf("toRunTrampolined", "toRunOnFrame")) (field(queue).get(dispatcher) as MutableCollection<*>).clear()
        for (flag in listOf("scheduledTrampolineDispatch", "scheduledFrameDispatch")) field(flag).setBoolean(dispatcher, false)
    }
}
