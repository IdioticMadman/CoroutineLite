package org.robert.sample

import org.robert.delay
import org.robert.launch
import org.robert.util.log
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun main() {
    val job = launch {
        log(1)
        log(2)
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