package org.robert.core

import org.robert.Job
import org.robert.OnCancel
import org.robert.OnComplete
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class AbstractCoroutine<T>(context: CoroutineContext) : Job, Continuation<T> {

    protected val state = AtomicReference<CoroutineState>()
    override val context = context + this

    protected val parentJob = context[Job]
    private var parentCancelDisposable: Disposable? = null

    init {
        state.set(CoroutineState.InComplete())
        parentCancelDisposable = parentJob?.invokeOnCancel {
            cancel()
        }
    }

    override fun resumeWith(result: Result<T>) {
        val newState = state.updateAndGet { prevState ->
            when (prevState) {
                is CoroutineState.Canceling,
                is CoroutineState.InComplete -> {
                    CoroutineState.Complete(result.getOrNull(), result.exceptionOrNull()).from(prevState)
                }
                is CoroutineState.Complete<*> -> {
                    throw IllegalStateException("already completed!")
                }
            }
        }
        //todo 处理异常
        newState.notifyCompletion(result)
        newState.clear()
    }

    override val isActive: Boolean
        get() = state.get() is CoroutineState.InComplete


    override val isComplete: Boolean
        get() = state.get() is CoroutineState.Complete<*>

    override fun invokeOnCompletion(onComplete: OnComplete): Disposable {
        return doOnCompleted {
            onComplete()
        }
    }

    override fun remove(disposable: Disposable) {
        state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.Complete<*> -> prev
                is CoroutineState.InComplete ->
                    CoroutineState.InComplete()
                        .from(prev)
                        .without(disposable)
                is CoroutineState.Canceling ->
                    CoroutineState.Canceling()
                        .from(prev)
                        .without(disposable)
            }
        }
    }

    override suspend fun join() {
        when (state.get()) {
            is CoroutineState.Canceling,
            is CoroutineState.InComplete -> return joinSuspend()
            is CoroutineState.Complete<*> -> return
        }
    }

    private suspend fun joinSuspend() = suspendCoroutine<Unit> { continuation ->
        doOnCompleted {
            continuation.resume(Unit)
        }
    }

    protected fun doOnCompleted(block: (Result<T>) -> Unit): Disposable {
        val disposable = CompletionHandlerDisposable(this, block)
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.InComplete -> {
                    CoroutineState.InComplete().from(prev).with(disposable)
                }
                is CoroutineState.Complete<*> -> {
                    prev
                }
                is CoroutineState.Canceling -> {
                    CoroutineState.Canceling().from(prev).with(disposable)
                }
            }
        }
        (newState as? CoroutineState.Complete<T>)?.let {
            block(
                when {
                    it.value != null -> Result.success(it.value)
                    it.t != null -> Result.failure(it.t)
                    else -> throw IllegalStateException("Won't happen")
                }
            )
        }
        return disposable
    }

    override fun cancel() {
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.InComplete -> {
                    CoroutineState.Canceling().from(prev)
                }
                is CoroutineState.Canceling,
                is CoroutineState.Complete<*> -> prev
            }
        }
        if (newState is CoroutineState.Canceling) {
            newState.notifyCancellation()
        }
        parentCancelDisposable?.dispose()
    }

    override fun invokeOnCancel(onCancel: OnCancel): Disposable {
        val disposable = CancelHandlerDisposable(this, onCancel)
        val newState = state.updateAndGet { prev ->
            when (prev) {
                is CoroutineState.InComplete -> {
                    CoroutineState.InComplete().from(prev).with(disposable)
                }
                is CoroutineState.Canceling,
                is CoroutineState.Complete<*> -> prev
            }
        }
        (newState as? CoroutineState.Canceling)?.let {
            onCancel()
        }
        return disposable
    }
}