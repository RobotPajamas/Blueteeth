package com.robotpajamas.blueteeth.models

typealias CompletionBlock<T> = (Result<T>, () -> Unit) -> Unit
typealias ExecutionBlock<T> = ((Result<T>) -> Unit) -> Unit

private enum class State {
    NONE, READY, EXECUTING, FINISHING, FINISHED
}

class QueueItem<T>(private val name: String? = null,
                   private val execution: ExecutionBlock<T>? = null,
                   private val completion: CompletionBlock<T>? = null) : Runnable, Queueable {


    private var state = State.READY
        set(value) {}

    override fun run() {
        if (isCancelled) {
        }
        state = State.EXECUTING
        execute()
    }

    private var isCancelled = false
    override fun cancel() {
        isCancelled = true
    }

    override fun execute() {
        execution?.invoke { result ->
            result.failure { notify(result) }
        } ?: done()
    }

    fun notify(result: Result<T>) {
        completion?.invoke(result) { done() } ?: done()
    }

    fun done() {
        state = State.FINISHED
    }

}