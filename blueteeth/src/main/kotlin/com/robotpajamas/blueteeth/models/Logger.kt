package com.robotpajamas.blueteeth.models

import com.robotpajamas.blueteeth.blueteethLogger

interface Logger {
    fun verbose(message: String)
    fun debug(message: String)
    fun info(message: String)
    fun warning(message: String)
    fun error(message: String)
}

object BLog {
    fun v(message: String, vararg args: Any?, tag: String = "Blueteeth") {
        blueteethLogger?.verbose(String.format("$tag: $message", args))
    }

    fun d(message: String, vararg args: Any?, tag: String = "Blueteeth") {
        blueteethLogger?.debug(String.format("$tag: $message", args))
    }

    fun i(message: String, vararg args: Any?, tag: String = "Blueteeth") {
        blueteethLogger?.info(String.format("$tag: $message", args))
    }

    fun w(message: String, vararg args: Any?, tag: String = "Blueteeth") {
        blueteethLogger?.warning(String.format("$tag: $message", args))
    }

    fun e(message: String, vararg args: Any?, tag: String = "Blueteeth") {
        blueteethLogger?.error(String.format("$tag: $message", args))
    }

}
