package org.robert

import org.robert.core.Disposable
import kotlin.coroutines.CoroutineContext

typealias OnComplete = () -> Unit
typealias OnCancel = () -> Unit

interface Job : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<Job>

    override val key: CoroutineContext.Key<*>
        get() = Job

    val isActive: Boolean

    val isComplete: Boolean

    //job 结束
    fun invokeOnCompletion(onComplete: OnComplete): Disposable

    fun remove(disposable: Disposable)

    suspend fun join()

    fun cancel()

    fun invokeOnCancel(onCancel: OnCancel): Disposable


}