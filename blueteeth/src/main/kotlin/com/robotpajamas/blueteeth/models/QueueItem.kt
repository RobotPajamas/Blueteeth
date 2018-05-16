package com.robotpajamas.blueteeth.models

import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException

typealias CompletionBlock<T> = (Result<T>) -> Unit
typealias ExecutionBlock<T> = ((Result<T>) -> Unit) -> Unit

private enum class State {
    NONE, READY, EXECUTING, FINISHING, FINISHED
}

class QueueItem<T>(val name: String = "QueueItem",
                   override val timeout: Int = 1,
                   private val execution: ExecutionBlock<T>,
                   completion: CompletionBlock<T>? = null) : Queueable<T> {
    private val completions = mutableListOf<CompletionBlock<T>>()

    init {
        assert(timeout >= 0, { "QueueItem timeout must be >= 0" })
        completion?.let { add(it) }
    }

    fun add(completion: CompletionBlock<T>) {
        completions.add(completion)
    }

    private var state = State.READY
        set(value) {}

    override fun run() {
        if (isCancelled) {
            complete(Result.Failure(CancellationException("$name: was cancelled before starting execution")))
            return
        }
        state = State.EXECUTING
        execute()
    }

    private var isCancelled = false
    override fun cancel() {
        isCancelled = true
    }

    override fun execute() {
        execution.invoke { result ->
            // Early return if failed
            result.failure { complete(result) }
        }
    }

    override fun complete(result: Result<T>) {
        completions.forEach { it.invoke(result) }
    }

    // Need this internal, because haven't figured out how to call Result.Failure from Dispatcher
    // with star-projected QueueItem type
    internal fun timeout() {
        complete(Result.Failure(TimeoutException("$name timed out after $timeout seconds")))
    }
}
