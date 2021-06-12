package org.robert.core

/**
 * 协程状态
 * 未完成
 * 已完成
 * 取消
 */
sealed class CoroutineState {

    private var disposableList: DisposableList = DisposableList.Nil

    fun from(state: CoroutineState): CoroutineState {
        this.disposableList = state.disposableList
        return this
    }

    fun with(disposable: Disposable): CoroutineState {
        this.disposableList = DisposableList.Cons(disposable, this.disposableList)
        return this
    }

    fun without(disposable: Disposable): CoroutineState {
        this.disposableList = this.disposableList.remove(disposable)
        return this
    }

    fun <T> notifyCompletion(result: Result<T>) {
        this.disposableList.loopOn<CompletionHandlerDisposable<T>> {
            it.onComplete(result)
        }
    }

    fun notifyCancellation() {
        this.disposableList.loopOn<CancelHandlerDisposable> {
            it.onCancel()
        }
    }

    fun clear() {
        this.disposableList = DisposableList.Nil
    }

    //未完成
    class InComplete : CoroutineState()

    //完成
    class Complete<T>(val value: T? = null, val t: Throwable? = null) : CoroutineState()

    //取消中
    class Canceling() : CoroutineState()
}