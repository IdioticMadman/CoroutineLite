package org.robert.core

import org.robert.Job
import org.robert.OnCancel
import org.robert.OnComplete
import org.robert.cancel.suspendCancellableCoroutine
import org.robert.context.CoroutineName
import org.robert.scope.CoroutineScope
import java.lang.IllegalStateException
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

abstract class AbstractCoroutine<T>(context: CoroutineContext) : Job, Continuation<T>, CoroutineScope {

    protected val state = AtomicReference<CoroutineState>()
    override val context = context + this
    override val scopeContext: CoroutineContext
        get() = context

    protected val parentJob = context[Job]
    private var parentCancelDisposable: Disposable? = null

    init {
        state.set(CoroutineState.InComplete())
        //父协程取消时，子协程也取消
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
        (newState as CoroutineState.Complete<T>).throwable?.let(::tryHandleException)
        newState.notifyCompletion(result)
        newState.clear()
        //子协程完成，取消在父协程注册的监听
        parentCancelDisposable?.dispose()
    }

    private fun tryHandleException(throwable: Throwable): Boolean {
        return when (throwable) {
            is CancellationException -> false
            else -> {
                //判断父协程是否需要处理异常
                (parentJob as? AbstractCoroutine<*>)?.handleChildException(throwable)?.takeIf { it }
                    ?: handleJobException(throwable)
            }
        }
    }

    protected open fun handleJobException(throwable: Throwable): Boolean {
        return false
    }

    protected open fun handleChildException(throwable: Throwable): Boolean {
        //子协程发生异常，需要取消子协程的父协程
        cancel()
        //然后继续往上抛
        return tryHandleException(throwable)
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
            is CoroutineState.Complete<*> -> {
                //父协程不是active状态，子协程抛
                val currentCallingJobState = coroutineContext[Job]?.isActive ?: return
                if (!currentCallingJobState) {
                    throw CancellationException("Coroutine is canceled")
                }
                return
            }
        }
    }

    private suspend fun joinSuspend() = suspendCancellableCoroutine<Unit> { continuation ->
        val disposable = doOnCompleted {
            continuation.resume(Unit)
        }
        //join方法支持取消
        continuation.invokeOnCancel {
            disposable.dispose()
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
                    it.throwable != null -> Result.failure(it.throwable)
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
        // 当前协程被取消，取消父协程取消的监听
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

    override fun toString(): String {
        return "${context[CoroutineName]?.name}"
    }
}