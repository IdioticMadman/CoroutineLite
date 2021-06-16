package org.robert.exception

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * 异常处理器的handler，实际上也是协程上下文
 */
interface CoroutineExceptionHandler : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<CoroutineExceptionHandler>

    fun handleException(context: CoroutineContext, throwable: Throwable)
}

/**
 * 将异常处理器添加到协程上下文中
 */
inline fun coroutineExceptionHandler(
    /**
     *
     */
    crossinline handler: (CoroutineContext, Throwable) -> Unit
): CoroutineExceptionHandler =
    object : AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
        override fun handleException(context: CoroutineContext, throwable: Throwable) {
            handler.invoke(context, throwable)
        }
    }