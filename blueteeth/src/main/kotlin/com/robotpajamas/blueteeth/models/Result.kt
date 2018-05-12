package com.robotpajamas.blueteeth.models

sealed class Result<Value> {
    abstract val value: Value?
    abstract val error: Exception?

    class Success<Value : Any>(override val value: Value) : Result<Value>() {
        override val error: Exception? = null
    }

    class Failure(override val error: Exception) : Result<Nothing>() {
        override val value: Nothing? = null
    }

    val isSuccess: Boolean
        get() = when (this) {
            is Result.Success -> true
            else -> false
        }

    val isFailure: Boolean
        get() = !isSuccess

    fun success(call: (Value) -> Unit) {
        if (this is Result.Success) {
            call(value)
        }
    }

    fun failure(call: (Exception) -> Unit) {
        if (this is Result.Failure) {
            call(error)
        }
    }

    @Throws(Exception::class)
    fun unwrap(): Value = when (this) {
        is Result.Success -> value
        is Result.Failure -> throw error
    }
}
