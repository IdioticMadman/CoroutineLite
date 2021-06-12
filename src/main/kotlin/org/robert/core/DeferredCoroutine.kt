package org.robert.core

import org.robert.Deferred
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine

@Suppress("UNCHECKED_CAST")
class DeferredCoroutine<T>(context: CoroutineContext) : AbstractCoroutine<T>(context), Deferred<T> {

    override suspend fun await(): T {
        return when (val coroutineState = state.get()) {
            is CoroutineState.Complete<*> -> (coroutineState.value as T?) ?: throw coroutineState.throwable!!
            is CoroutineState.Canceling,
            is CoroutineState.InComplete -> awaitSuspend()
        }
    }

    private suspend fun awaitSuspend(): T = suspendCoroutine { continuation ->
        doOnCompleted { result ->
            continuation.resumeWith(result)
        }
    }
}