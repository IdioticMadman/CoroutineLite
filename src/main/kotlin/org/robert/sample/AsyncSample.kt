package org.robert.sample

import org.robert.async
import org.robert.delay
import org.robert.runBlocking
import org.robert.util.log

fun main() = runBlocking {
    log(1)
    val deferred = async {
        log(2)
        delay(200)
        log(3)
    }
    log(4)
    deferred.await()
    log(5)
}