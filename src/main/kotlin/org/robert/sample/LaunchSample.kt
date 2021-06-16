package org.robert.sample

import org.robert.Job
import org.robert.delay
import org.robert.exception.coroutineExceptionHandler
import org.robert.launch
import org.robert.scope.CoroutineScope
import org.robert.scope.GlobalScope
import org.robert.scope.supervisorScope
import org.robert.util.log
import java.lang.ArithmeticException
import java.lang.IllegalStateException
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun main() {
    val handler = coroutineExceptionHandler { coroutineContext, throwable ->
        log(coroutineContext[Job], throwable)
    }
    val job = GlobalScope.launch(handler) {
        log(1)
        delay(200)
        supervisorScope {
            log(2)
            val job1 = launch(handler) {
                throw ArithmeticException("div 0")
            }
            log(3)
            job1.join()
        }
        log(4)
    }
    log(job.isActive)
    job.join()
}

suspend fun hello() = suspendCoroutine<Int> {
    thread(isDaemon = true) {
        Thread.sleep(1000)
        it.resume(3)
    }
}