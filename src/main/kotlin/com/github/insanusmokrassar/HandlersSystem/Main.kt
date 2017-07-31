package com.github.insanusmokrassar.HandlersSystem

import com.github.insanusmokrassar.utils.loadLoggerConfig

fun main(args: Array<String>) {
    args.forEach {
        println(it)
    }
    loadLoggerConfig()
}