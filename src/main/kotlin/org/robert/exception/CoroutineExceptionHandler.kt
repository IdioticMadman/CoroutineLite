package org.robert.exception

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

interface CoroutineExceptionHandler : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<CoroutineExceptionHandler>

    fun handleException(context: CoroutineContext, throwable: Throwable)
}

inline fun coroutineExceptionHandler(
    crossinline handler: (CoroutineContext, Throwable) -> Unit
): CoroutineExceptionHandler =
    object : AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
        override fun handleException(context: CoroutineContext, throwable: Throwable) {
            handler.invoke(context, throwable)
        }
    }