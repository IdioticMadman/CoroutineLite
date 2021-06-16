package org.robert

import org.robert.context.CoroutineName
import org.robert.core.BlockCoroutine
import org.robert.core.BlockingQueueDispatcher
import org.robert.core.DeferredCoroutine
import org.robert.core.StandardCoroutine
import org.robert.dispatcher.DispatcherContext
import org.robert.dispatcher.Dispatchers
import org.robert.scope.CoroutineScope
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

private var coroutineIndex = AtomicInteger(0)

fun CoroutineScope.launch(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> Unit
): Job {
    val completion = StandardCoroutine(newCoroutineContext(context))
    block.startCoroutine(completion, completion)
    return completion
}

fun <T> CoroutineScope.async(
    context: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
    val completion = DeferredCoroutine<T>(newCoroutineContext(context))
    block.startCoroutine(completion, completion)
    return completion
}

fun CoroutineScope.newCoroutineContext(context: CoroutineContext): CoroutineContext {
    val combined = scopeContext + context + CoroutineName("@coroutine${coroutineIndex.getAndIncrement()}")
    if (context !== Dispatchers.Default && context[ContinuationInterceptor] == null) {
        return Dispatchers.Default + combined
    }
    return combined
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
    //开启协程的时候，第一个resume就会入队列，等待joinBlocking进行从队列中读取runnable
    block.startCoroutine(completion)
    return completion.joinBlocking()
}

