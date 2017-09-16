package com.github.insanusmokrassar.HandlersSystem.core

import com.github.insanusmokrassar.HandlersSystem.nameField
import com.github.insanusmokrassar.IOC.core.IOCStrategy
import com.github.insanusmokrassar.IObjectK.interfaces.IObject

class HandlerIOCStrategy(config: List<IObject<Any>>): IOCStrategy {
    private val handlers: Map<String, Handler>

    init {
        val futureHandlers = HashMap<String, Handler>()
        config.forEach {
            futureHandlers.put(
                    it.get(nameField),
                    loadHandler(it)
            )
        }
        handlers = futureHandlers
    }

    override fun <T : Any> getInstance(vararg args: Any): T {
        return handlers[args[0]] as T
    }
}