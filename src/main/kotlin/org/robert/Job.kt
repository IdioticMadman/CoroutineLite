package org.robert

import org.robert.core.Disposable
import kotlin.coroutines.CoroutineContext

typealias OnComplete = () -> Unit
typealias OnCancel = () -> Unit

interface Job : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<Job>

    override val key: CoroutineContext.Key<*>
        get() = Job

    /**
     * 是否处于激活状态，即当前处于
     * [CoroutineState.InComplete]
     */
    val isActive: Boolean

    /**
     * 是否处于完成状态状态，即当前处于
     * [CoroutineState.Complete]
     */
    val isComplete: Boolean

    /**
     * 当前协程执行完成回调onCancel
     * 可通过[Disposable.dispose] 进行取消监听
     */
    fun invokeOnCompletion(onComplete: OnComplete): Disposable

    /**
     * 移除监听
     */
    fun remove(disposable: Disposable)

    /**
     * 挂起，等待当前协程执行完毕
     */
    suspend fun join()

    /**
     * 取消当前协程
     */
    fun cancel()

    /**
     * 当前协程被取消回调onCancel
     * 可通过[Disposable.dispose] 进行取消监听
     */
    fun invokeOnCancel(onCancel: OnCancel): Disposable


}