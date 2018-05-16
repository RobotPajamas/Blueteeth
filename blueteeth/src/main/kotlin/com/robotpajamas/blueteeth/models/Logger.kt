package com.robotpajamas.blueteeth.models

import android.util.Log
import com.robotpajamas.blueteeth.blueteethLogger

interface Logger {
    fun verbose(message: String)
    fun debug(message: String)
    fun info(message: String)
    fun warning(message: String)
    fun error(message: String)
}

object BLog {
    fun v(message: String, tag: String = "Blueteeth") {
        blueteethLogger?.verbose("$tag: $message")
    }

    fun d(message: String, tag: String = "Blueteeth") {
        blueteethLogger?.debug("$tag: $message")
    }

    fun i(message: String, tag: String = "Blueteeth") {
        blueteethLogger?.info("$tag: $message")
    }

    fun w(message: String, tag: String = "Blueteeth") {
        blueteethLogger?.warning("$tag: $message")
    }

    fun e(message: String, tag: String = "Blueteeth") {
        blueteethLogger?.error("$tag: $message")
    }

}
