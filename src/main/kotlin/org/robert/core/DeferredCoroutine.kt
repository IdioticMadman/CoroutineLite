package org.robert.core

import org.robert.Deferred
import org.robert.Job
import org.robert.cancel.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.suspendCoroutine

@Suppress("UNCHECKED_CAST")
class DeferredCoroutine<T>(context: CoroutineContext) : AbstractCoroutine<T>(context), Deferred<T> {

    override suspend fun await(): T {
        return when (val coroutineState = state.get()) {
            is CoroutineState.Complete<*> -> {
                coroutineContext[Job]?.isActive?.takeIf { !it }?.let {
                    throw CancellationException("Coroutine is cancelled")
                }
                (coroutineState.value as T?) ?: throw coroutineState.throwable!!
            }
            is CoroutineState.Canceling,
            is CoroutineState.InComplete -> awaitSuspend()
        }
    }

    private suspend fun awaitSuspend(): T = suspendCancellableCoroutine { continuation ->
        val disposable = doOnCompleted { result ->
            continuation.resumeWith(result)
        }
        continuation.invokeOnCancel {
            disposable.dispose()
        }
    }
}