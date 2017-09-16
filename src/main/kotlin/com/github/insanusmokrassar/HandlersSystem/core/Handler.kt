package com.github.insanusmokrassar.HandlersSystem.core

import com.github.insanusmokrassar.HandlersSystem.classPathField
import com.github.insanusmokrassar.IOC.core.getConfig
import com.github.insanusmokrassar.IOC.utils.extract
import com.github.insanusmokrassar.IObjectK.interfaces.IObject

interface Handler {
    fun handle(params: IObject<Any>)
}

/**
 * Await:
 * <p>
 *  "classPath" - String, which contains classpath to handler realisation
 * </p>
 * <p>
 *  "params" - optional object, will translate into handler constructor
 * </p>
 */
fun loadHandler(config: IObject<Any>): Handler {
    val classPath = config.get<String>(classPathField)
    val args = getConfig(config)
    return extract(classPath, *args)
}