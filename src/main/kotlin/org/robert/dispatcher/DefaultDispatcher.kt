package org.robert.dispatcher

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object DefaultDispatcher : Dispatcher {

    private val threadGroup = ThreadGroup("DefaultDispatcher")
    private val threadIndex = AtomicInteger(0)

    private val executor =
        Executors.newFixedThreadPool(
            2 * Runtime.getRuntime().availableProcessors()
        ) { runnable ->
            Thread(
                threadGroup, runnable,
                "${threadGroup.name}-worker-${threadIndex.getAndIncrement()}"
            ).also { it.isDaemon = true }
        }

    override fun dispatch(block: () -> Unit) {
        executor.submit(block)
    }
}