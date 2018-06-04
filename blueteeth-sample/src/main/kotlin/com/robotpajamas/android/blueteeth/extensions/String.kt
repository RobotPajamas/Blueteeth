package com.robotpajamas.android.blueteeth.extensions

fun String.prepend(message: String): String {
    return "$message$this"
}