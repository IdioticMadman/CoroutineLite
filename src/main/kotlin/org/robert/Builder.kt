package org.robert

import org.robert.context.CoroutineName
import org.robert.core.BlockCoroutine
import org.robert.core.BlockingQueueDispatcher
import org.robert.core.DeferredCoroutine
import org.robert.core.StandardCoroutine
import org.robert.dispatcher.DispatcherContext
import org.robert.dispatcher.Dispatchers
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

private var coroutineIndex = AtomicInteger(0)

fun launch(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> Unit
): Job {
    val completion = StandardCoroutine(newCoroutineContext(context))
    block.startCoroutine(completion)
    return completion
}

fun newCoroutineContext(context: CoroutineContext): CoroutineContext {
    val combined = context + CoroutineName("@coroutine${coroutineIndex.getAndIncrement()}")
    if (context !== Dispatchers.Default && context[ContinuationInterceptor] == null) {
        return Dispatchers.Default + combined
    }
    return combined
}

/**
 * 使用阻塞队列进行挂起恢复
 */
fun <T> runBlocking(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> T
): T {
    val dispatcher = BlockingQueueDispatcher()
    val eventQueueContext = newCoroutineContext(context + DispatcherContext(dispatcher))
    val completion = BlockCoroutine<T>(eventQueueContext, dispatcher)
    block.startCoroutine(completion)
    return completion.joinBlocking()
}

fun <T> async(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend () -> T
): Deferred<T> {
    val completion = DeferredCoroutine<T>(newCoroutineContext(context))
    block.startCoroutine(completion)
    return completion
}