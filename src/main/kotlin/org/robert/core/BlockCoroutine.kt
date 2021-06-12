package org.robert.core

import org.robert.dispatcher.Dispatcher
import java.util.concurrent.LinkedBlockingDeque
import kotlin.coroutines.CoroutineContext

typealias EventTask = () -> Unit

class BlockingQueueDispatcher : LinkedBlockingDeque<EventTask>(), Dispatcher {
    override fun dispatch(block: () -> Unit) {
        //恢复时，添加到阻塞队列
        offer(block)
    }
}

class BlockCoroutine<T>(
    context: CoroutineContext,
    private val eventQueue: BlockingQueueDispatcher
) : AbstractCoroutine<T>(context) {
    //阻塞
    fun joinBlocking(): T {
        while (!isComplete) {
            //循环读取阻塞队列
            eventQueue.take().invoke()
        }
        return (state.get() as CoroutineState.Complete<T>)?.let {
            it.value ?: throw it.throwable!!
        }
    }
}