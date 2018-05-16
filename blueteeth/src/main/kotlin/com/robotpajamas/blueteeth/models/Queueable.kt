package com.robotpajamas.blueteeth.models

interface Queueable {
    fun execute()
    fun cancel()
}