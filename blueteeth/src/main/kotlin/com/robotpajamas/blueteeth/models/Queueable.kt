package com.robotpajamas.blueteeth.models

interface Queueable<T> : Runnable {
    val timeout: Int

    fun cancel()
    fun execute()
    fun complete(result: Result<T>)
}