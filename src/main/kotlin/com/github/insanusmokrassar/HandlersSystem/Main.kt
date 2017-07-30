package com.github.insanusmokrassar.HandlersSystem

import com.github.insanusmokrassar.iobjectk.loadLoggerConfig

fun main(args: Array<String>) {
    args.forEach {
        println(it)
    }
    loadLoggerConfig()
}