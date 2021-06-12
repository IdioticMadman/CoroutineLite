package org.robert.core

import org.robert.exception.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

class StandardCoroutine(context: CoroutineContext) : AbstractCoroutine<Unit>(context) {

    override fun handleJobException(throwable: Throwable): Boolean {
        context[CoroutineExceptionHandler]?.handleException(context, throwable)
            ?: Thread.currentThread().let {
                it.uncaughtExceptionHandler.uncaughtException(it, throwable)
            }
        return true
    }
}