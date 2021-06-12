package org.robert.cancel

sealed class CancelState {
    object InComplete : CancelState()
    class Complete<T>(val value: T? = null, val throwable: Throwable? = null) : CancelState()
    object Cancelled : CancelState()

    override fun toString(): String {
        return "CancelState.${javaClass.simpleName}"
    }
}