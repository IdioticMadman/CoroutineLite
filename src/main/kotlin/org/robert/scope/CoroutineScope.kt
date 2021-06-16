package org.robert.scope

import org.robert.Job
import org.robert.core.AbstractCoroutine
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

interface CoroutineScope {
    val scopeContext: CoroutineContext
}

internal class ContextScope(context: CoroutineContext) : CoroutineScope {
    override val scopeContext: CoroutineContext = context
}

operator fun CoroutineScope.plus(context: CoroutineContext): CoroutineScope =
    ContextScope(scopeContext + context)

fun CoroutineScope.cancel() {
    val job = scopeContext[Job] ?: error("Scope cannot be cancelled because it does not has job")
    job.cancel()
}

suspend fun <R> coroutineScope(block: suspend CoroutineScope.() -> R): R =
    suspendCoroutine { continuation ->
        val coroutine = ScopeCoroutine(continuation.context, continuation)
        block.startCoroutine(coroutine, coroutine)
    }

open class ScopeCoroutine<T>(context: CoroutineContext, private val continuation: Continuation<T>) :
    AbstractCoroutine<T>(context) {
    override fun resumeWith(result: Result<T>) {
        super.resumeWith(result)
        continuation.resumeWith(result)
    }
}