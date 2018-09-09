//package com.robotpajamas.blueteeth.internal
//
//import android.os.Handler
//import android.os.Looper
//import com.robotpajamas.blueteeth.models.QueueItem
//import java.util.*
//import java.util.concurrent.ConcurrentLinkedQueue
//import java.util.concurrent.Executor
//
//internal class Dispatcher {
//
//    // Should be a Queueable?
//    private var active: QueueItem<*>? = null
//    private val queue: Queue<QueueItem<*>> = ConcurrentLinkedQueue()
//    private val dispatchHandler = Handler(Looper.getMainLooper())
//
//    private val executor: Executor = object : Executor {
//        private val handler = Handler(Looper.getMainLooper())
//        override fun execute(command: Runnable) {
//            handler.post(command)
//        }
//    }
//
//    @Synchronized
//    fun clear() {
//        // TODO: What to do with in-flight calls? Time out? Cancel?
//        dispatchHandler.removeCallbacksAndMessages(null)
//        active = null
//        queue.clear()
//    }
//
//    @Synchronized
//    fun enqueue(item: QueueItem<*>) {
//        // Append a completion callback
//        item.add { dispatchNext() }
//        queue.add(item)
//        if (active == null) {
//            dispatchNext()
//        }
//    }
//
//    @Synchronized
//    private fun dispatchNext() {
//        // Remove pending timeout runnables
//        dispatchHandler.removeCallbacksAndMessages(null)
//
//        active = queue.poll()
//        active?.let {
//            // Start timeout clock
//            val cancel = Runnable {
//                // TODO: Check if this is doing what I think it is
//                // Capture this current queueItem, and compare it against the active item in X seconds
//                if (it.name != active?.name) {
//                    return@Runnable
//                }
//                active?.timeout()
//            }
//            dispatchHandler.postDelayed(cancel, it.timeout * 1000L)
//            executor.execute(it)
//        }
//    }
//
//    @Synchronized
//    internal fun <T> dispatched(name: String): QueueItem<T>? {
//        @Suppress("UNCHECKED_CAST")
//        return if (name == active?.name) active as? QueueItem<T> else null
//    }
//}
