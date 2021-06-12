package org.robert.dispatcher.ui

import org.robert.dispatcher.Dispatcher
import javax.swing.SwingUtilities

object SwingDispatcher : Dispatcher {
    override fun dispatch(block: () -> Unit) {
        SwingUtilities.invokeLater(block)
    }

}