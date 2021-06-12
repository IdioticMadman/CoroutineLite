package org.robert.dispatcher.ui

import android.os.Handler
import android.os.Looper
import org.robert.dispatcher.Dispatcher

object HandlerDispatcher : Dispatcher {
    private val handler = Handler(Looper.getMainLooper())
    override fun dispatch(block: () -> Unit) {
        handler.post(block)
    }
}