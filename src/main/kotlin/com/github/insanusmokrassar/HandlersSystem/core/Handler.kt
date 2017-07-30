package com.github.insanusmokrassar.HandlersSystem.core

import com.github.insanusmokrassar.HandlersSystem.classPathField
import com.github.insanusmokrassar.HandlersSystem.paramsField
import com.github.insanusmokrassar.IOC.utils.extract
import com.github.insanusmokrassar.iobjectk.exceptions.ReadException
import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import java.util.logging.Logger

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
    if (config.keys().contains(paramsField)) {
        try {
            val params = config.get<List<Any>>(paramsField)
            return extract(classPath, *params.toTypedArray())
        } catch (e: ReadException) {
            Logger.getGlobal().info("Params is not list, try as common params")
            val params = config.get<Any>(paramsField)
            return extract(classPath, params)
        }
    } else {
        return extract(classPath)
    }
}