package com.github.insanusmokrassar.HandlersSystem.core

import com.github.insanusmokrassar.HandlersSystem.*
import com.github.insanusmokrassar.IOC.core.IOCStrategy
import com.github.insanusmokrassar.IOC.core.getOrCreateIOC
import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectK.realisations.SimpleIObject

class HandlersMapIOCStrategy(config: IObject<Any>): IOCStrategy {

    private val maps: Map<String?, HandlersMap>

    /**
     * Await:
     * <pre>
     *     {
     *         "maps": [
     *             //Look at {@link HandlersMap}
     *         ],
     *         "systemConfig": Some object with system configuration
     *         "systemConfigStrategy": "or name of strategy to resolve config",
     *         "IOCName": "name of ioc if need to resolve by strategy"
     *     }
     * </pre>
     */
    init {
        val handlersMapsConfigs = config.get<List<IObject<Any>>>(mapsField)
        val systemConfig = if (config.keys().contains(systemConfigObjectField)) {
            config[systemConfigObjectField]
        } else {
            if (config.keys().contains(systemConfigStrategyField)) {
                getOrCreateIOC(
                        config[IOCNameField]
                ).resolve<IObject<Any>>(
                        config[systemConfigStrategyField],
                        systemConfigObjectField
                )
            } else {
                SimpleIObject()
            }
        }
        val handlersMapsList = HashMap<String?, HandlersMap>()
        handlersMapsConfigs.forEach {
            if (!it.keys().contains(IOCNameField)) {
                it[IOCNameField] = config[IOCNameField]
            }
            try {
                handlersMapsList[it[nameField]] = HandlersMap(it, systemConfig)
            } catch (e: ReadException) {
                handlersMapsList[null] = HandlersMap(it, systemConfig)
            }
        }
        maps = handlersMapsList
    }

    override fun <T : Any> getInstance(vararg args: Any?): T {
        return if (args.isEmpty() || args[0] == null) {
            maps[null] as T
        } else {
            maps[args[0]] as T
        }
    }
}