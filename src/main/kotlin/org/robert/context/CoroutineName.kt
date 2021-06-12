package org.robert.context

import kotlin.coroutines.CoroutineContext

class CoroutineName(val name: String) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<CoroutineName>

    override val key: CoroutineContext.Key<*>
        get() = Key

    override fun toString(): String {
        return name
    }
}