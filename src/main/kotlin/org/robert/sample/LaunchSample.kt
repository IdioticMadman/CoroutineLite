package org.robert.sample

import org.robert.Job
import org.robert.delay
import org.robert.exception.coroutineExceptionHandler
import org.robert.launch
import org.robert.util.log
import java.lang.IllegalStateException
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun main() {

    val job = launch(coroutineExceptionHandler { coroutineContext, throwable ->
        log(coroutineContext[Job],  throwable)
    }) {
        log(1)
        log(2)
        throw IllegalStateException("state error")
        val ret = hello()
        log(ret)
        delay(2000)
        log(4)
    }
    log(job.isActive)
    job.cancel()
    log(job.isActive)
    job.join()
}

suspend fun hello() = suspendCoroutine<Int> {
    thread(isDaemon = true) {
        Thread.sleep(1000)
        it.resume(3)
    }
}