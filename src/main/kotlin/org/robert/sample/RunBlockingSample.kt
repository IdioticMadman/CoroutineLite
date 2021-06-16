package org.robert.sample

import jdk.nashorn.internal.objects.Global
import org.robert.delay
import org.robert.launch
import org.robert.runBlocking
import org.robert.scope.GlobalScope
import org.robert.util.log

fun main() = runBlocking {
    log("0")
    val job = GlobalScope.launch {
        log(1)
        log(2)
        val ret = hello()
        log(ret)
        delay(2000)
        log(4)
    }
    log(5)
    log(job.isActive)
    job.join()
    log(6)
    delay(2000)
    log(7)
}