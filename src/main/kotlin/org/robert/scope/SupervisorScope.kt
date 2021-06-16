package org.robert.scope

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

class SupervisorCoroutine<T>(context: CoroutineContext, continuation: Continuation<T>) :
    ScopeCoroutine<T>(context, continuation) {

    override fun handleChildException(throwable: Throwable): Boolean {
        //主从协程，子协程不影响父协程
        return false
    }
}

suspend fun <R> supervisorScope(block: suspend CoroutineScope.() -> R): R =
    suspendCoroutine { continuation ->
        val coroutine = SupervisorCoroutine(continuation.context, continuation)
        block.startCoroutine(coroutine, continuation)
    }