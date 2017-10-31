package com.github.insanusmokrassar.HandlersSystem.core

import com.github.insanusmokrassar.IOC.core.getConfig
import com.github.insanusmokrassar.IOC.core.packageKey
import com.github.insanusmokrassar.IOC.utils.extract
import com.github.insanusmokrassar.IObjectK.interfaces.IObject

interface Handler {
    fun handle(requestParams: IObject<Any>)
}

/**
 * Await:
 * <p>
 *  "package" - String, which contains classpath to handler realisation
 * </p>
 * <p>
 *  "config" - optional object, will translate into handler constructor
 * </p>
 */
fun loadHandler(config: IObject<Any>): Handler = extract(config.get(packageKey), *getConfig(config))
