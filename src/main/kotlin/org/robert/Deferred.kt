package org.robert

interface Deferred<T> : Job {
    suspend fun await(): T
}