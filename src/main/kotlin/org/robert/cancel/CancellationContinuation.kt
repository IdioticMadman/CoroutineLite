package org.robert.cancel

import org.robert.Job
import org.robert.OnCancel
import java.lang.IllegalStateException
import java.util.concurrent.CancellationException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resumeWithException

class CancellationContinuation<T>(private val continuation: Continuation<T>) :
    Continuation<T> by continuation {

    private val state = AtomicReference<CancelState>(CancelState.InComplete)

    private val cancelHandlers = CopyOnWriteArrayList<OnCancel>()

    val isCompleted: Boolean
        get() = state.get() is CancelState.Complete<*>

    val isActive: Boolean
        get() = state.get() == CancelState.InComplete

    fun cancel() {
        if (!isActive) return
        val parent = continuation.context[Job] ?: return
        parent.cancel()
    }

    fun invokeOnCancel(onCancel: OnCancel) {
        cancelHandlers + onCancel
    }

    override fun resumeWith(result: Result<T>) {
        state.updateAndGet { prev ->
            when (prev) {
                CancelState.InComplete -> {
                    continuation.resumeWith(result)
                    CancelState.Complete(result.getOrNull(), result.exceptionOrNull())
                }
                CancelState.Cancelled -> {
                    CancellationException("Cancelled").let {
                        continuation.resumeWithException(it)
                        CancelState.Complete(null, it)
                    }
                }
                is CancelState.Complete<*> -> throw IllegalStateException("Already completed")
            }
        }
    }

    fun getResult(): Any? {
        //注册监听
        installCancelHandler()
        return when (val currentState = state.get()) {
            CancelState.Cancelled -> throw CancellationException("Continuation is canceled")
            CancelState.InComplete -> COROUTINE_SUSPENDED
            is CancelState.Complete<*> -> {
                (currentState as CancelState.Complete<T>).let {
                    it.throwable?.let { throw it } ?: it.value
                }
            }
        }
    }

    private fun installCancelHandler() {
        if (!isActive) return
        //获取当前context中的job
        val parent = continuation.context[Job] ?: return
        //给当前job添加取消监听，当job调用cancel后，也取消当前Continuation
        parent.invokeOnCancel {
            doCancel()
        }
    }

    private fun doCancel() {
        state.updateAndGet { prev ->
            when (prev) {
                CancelState.InComplete -> CancelState.Cancelled
                CancelState.Cancelled,
                is CancelState.Complete<*> -> prev
            }
        }
        cancelHandlers.forEach(OnCancel::invoke)
        cancelHandlers.clear()
    }
}

suspend inline fun <T> suspendCancellableCoroutine(
    crossinline block: (CancellationContinuation<T>) -> Unit
): T = suspendCoroutineUninterceptedOrReturn { c: Continuation<T> ->
    val cancellationContinuation = CancellationContinuation(c.intercepted())
    //执行协程体体
    block(cancellationContinuation)
    //获取当前协程执行结果，并监听调用这个挂起的Job是否取消
    cancellationContinuation.getResult()
}