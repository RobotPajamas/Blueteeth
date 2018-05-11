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
}

inline val Result<*>.isSuccess: Boolean
    get() {
        return when (this) {
            is Result.Success -> true
            else -> false
        }
    }

inline val Result<*>.isFailure: Boolean
    get() = !isSuccess

@Throws(Exception::class)
fun <Value> Result<Value>.unwrap(): Value {
    when (this) {
        is Result.Success -> return value
        is Result.Failure -> throw error
    }
}

fun <Value> Result<Value>.success(call: (Value) -> Unit) {
    if (this is Result.Success) {
        call(value)
    }
}

fun <Value> Result<Value>.failure(call: (Exception) -> Unit) {
    if (this is Result.Failure) {
        call(error)
    }
}