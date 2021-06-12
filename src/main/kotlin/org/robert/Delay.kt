package org.robert

import org.robert.cancel.suspendCancellableCoroutine
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val executor = Executors.newScheduledThreadPool(1) { runnable ->
    Thread(runnable).also {
        it.name = "Delay-Scheduler"
        it.isDaemon = true
    }
}

suspend fun delay(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS) =
    suspendCancellableCoroutine<Unit> { continuation ->
        val future = executor.schedule({
            continuation.resume(Unit)
        }, time, unit)
        continuation.invokeOnCancel {
            future.cancel(true)
        }
    }