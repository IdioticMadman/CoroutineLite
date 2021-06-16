package org.robert.dispatcher

import org.robert.dispatcher.ui.HandlerDispatcher
import org.robert.dispatcher.ui.SwingDispatcher

/**
 * 分发器的静态实例
 */
object Dispatchers {
    val Default by lazy {
        DispatcherContext(DefaultDispatcher)
    }

    val Android by lazy {
        DispatcherContext(HandlerDispatcher)
    }

    val Swing by lazy {
        DispatcherContext(SwingDispatcher)
    }

}