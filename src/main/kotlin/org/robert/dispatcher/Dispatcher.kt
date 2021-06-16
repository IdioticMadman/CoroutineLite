package org.robert.dispatcher

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor

interface Dispatcher {
    fun dispatch(block: () -> Unit)
}

/**
 * 协程分发Context，实现[AbstractCoroutineContextElement]和[ContinuationInterceptor]
 * 协程在开启的时候会根据[ContinuationInterceptor]进行索引加进去的拦截器
 */
open class DispatcherContext(private val dispatcher: Dispatcher) :
    AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
        DispatchedContinuation(continuation, dispatcher)
}

/**
 * 包装分发器的Continuation
 */
private class DispatchedContinuation<T>(
    val delegate: Continuation<T>,
    val dispatcher: Dispatcher
) : Continuation<T> {
    override val context = delegate.context
    override fun resumeWith(result: Result<T>) {
        dispatcher.dispatch {
            delegate.resumeWith(result)
        }
    }
}
