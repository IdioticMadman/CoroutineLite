package org.robert.core

/**
 * 协程状态
 * 未完成
 * 已完成
 * 取消
 */
sealed class CoroutineState {

    /**
     * 监听列表
     */
    private var disposableList: DisposableList = DisposableList.Nil

    /**
     * 从之前copy一份
     */
    fun from(state: CoroutineState): CoroutineState {
        this.disposableList = state.disposableList
        return this
    }

    /**
     * 添加
     */
    fun with(disposable: Disposable): CoroutineState {
        this.disposableList = DisposableList.Cons(disposable, this.disposableList)
        return this
    }

    /**
     * 移除
     */
    fun without(disposable: Disposable): CoroutineState {
        this.disposableList = this.disposableList.remove(disposable)
        return this
    }

    /**
     * 通知协程完成
     */
    fun <T> notifyCompletion(result: Result<T>) {
        this.disposableList.loopOn<CompletionHandlerDisposable<T>> {
            it.onComplete(result)
        }
    }

    /**
     * 通知协程取消
     */
    fun notifyCancellation() {
        this.disposableList.loopOn<CancelHandlerDisposable> {
            it.onCancel()
        }
    }

    /**
     * 清除回调
     */
    fun clear() {
        this.disposableList = DisposableList.Nil
    }

    //未完成
    class InComplete : CoroutineState()

    //完成
    class Complete<T>(
        val value: T? = null,
        val throwable: Throwable? = null
    ) : CoroutineState()

    //取消中
    class Canceling() : CoroutineState()
}