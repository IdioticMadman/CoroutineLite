package org.robert.core

import org.robert.exception.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

class StandardCoroutine(context: CoroutineContext) : AbstractCoroutine<Unit>(context) {

    override fun handleJobException(throwable: Throwable): Boolean {
        //在当前上下文中，找出异常处理器，进行异常抛出；如未有异常处理器，则丢给当前线程
        context[CoroutineExceptionHandler]?.handleException(context, throwable)
            ?: Thread.currentThread().let {
                it.uncaughtExceptionHandler.uncaughtException(it, throwable)
            }
        return true
    }
}