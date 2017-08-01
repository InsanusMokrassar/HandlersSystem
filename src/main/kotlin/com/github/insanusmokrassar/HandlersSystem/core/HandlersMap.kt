package com.github.insanusmokrassar.HandlersSystem.core

import com.github.insanusmokrassar.HandlersSystem.*
import com.github.insanusmokrassar.iobjectk.interfaces.IInputObject
import com.github.insanusmokrassar.iobjectk.interfaces.IObject
import com.github.insanusmokrassar.iobjectk.interfaces.addAll
import com.github.insanusmokrassar.iobjectk.interfaces.has
import com.github.insanusmokrassar.iobjectk.realisations.SimpleIObject
import com.github.insanusmokrassar.utils.IOC.IOC
import com.github.insanusmokrassar.utils.IOC.getOrCreateIOCInstance
import com.github.insanusmokrassar.utils.IOC.strategies.CacheIOCStrategy
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.logging.Logger

/**
 * Await that IOC was initialised to create instances of HandlersMap class and put into system config.
 * @param config object which contains:
 * <pre>
 *     "maps": [
 *          {
 *              ...// use handlers map template
 *          }
 *     ]
 * </pre>
 */
fun loadMaps(systemConfigObject: IObject<Any>, config: IObject<Any> = systemConfigObject) {
    val ioc = getOrCreateIOCInstance(systemConfigObject.get<String>(IOCNameField))

    try {
        config.get<List<IObject<Any>>>(mapsField).forEach {
            ioc.resolve(CacheIOCStrategy::class.simpleName!!, it.get<String>(nameField), HandlersMap::class.qualifiedName!!, systemConfigObject, it)
        }
    } catch (e: Exception) {
        val config = SimpleIObject()
        config.put(IOCNameField, ioc)
        val strategy = CacheIOCStrategy(config)
        ioc.register(CacheIOCStrategy::class.simpleName!!, strategy)
        loadMaps(systemConfigObject, config)
    }
}

/**
 * @param systemConfigObject Receive as first param object with settings of system: IOC object and other params which can be neede by handlers.
 * @param config object which contains:
 * <pre>
 *     {
 *          "name": "ExampleHandlersMapName",//required
 *          "path": [// required, contain path of map
 *              {
 *                  "handler": "Name of handler",// |handler or map field set the name of next "handler". If set handler - work as common, if map - set the context in request of map
 *                  "map": "or name of map",//      |
 *                  "config": {//optionally, will added when will executing as context params
 *                      ...
 *                  }
 *              }
 *          ],
 *          "config": {//optionally, will added when will executing as context params
 *              ...
 *          },
 *          "threadsGroupName": "common"//optionally, will using to get executor service by executor service strategy with the name
 *     }
 * </pre>
 */
class HandlersMap(private val systemConfigObject: IObject<Any>, private val config: IInputObject<String, Any>) {

    private val executor: ExecutorService

    init {
        if (config.has(threadsGroupNameField)) {
            executor = try {
                getOrCreateIOCInstance(systemConfigObject.get<String>(IOCNameField)).resolve<ExecutorService>(
                        ExecutorService::class.simpleName!!,
                        threadsGroupNameField
                )
            } catch (e: Exception) {
                Executors.newSingleThreadExecutor()
            }
        } else {
            executor = Executors.newSingleThreadExecutor()
        }
    }

    private val configObject: IObject<Any> = {
        if (config.keys().contains(configField)) {
            config.get(configField)
        } else {
            SimpleIObject()
        }
    }()

    fun execute(requestParams: IObject<Any>): Future<IObject<Any>> {
        return executor.submit(Callable<IObject<Any>>{
            val map = config.get<List<IObject<Any>>>(pathField)
            val handlersParamsObject = SimpleIObject()
            handlersParamsObject.put(systemConfigObjectField, systemConfigObject)
            handlersParamsObject.put(contextObjectField, SimpleIObject(configObject))
            handlersParamsObject.put(requestObjectField, requestParams)
            handlersParamsObject.put(resultObjectField, SimpleIObject())
            val ioc = getOrCreateIOCInstance(systemConfigObject.get<String>(IOCNameField))
            map.forEach {
                if (it.has(paramsField)) {
                    handlersParamsObject.put(
                            contextObjectField,
                            it.get<IObject<Any>>(paramsField).addAll(
                                    handlersParamsObject.get(contextObjectField)
                            )
                    )
                }
                if (it.has(handlerField)) {
                    try {
                        val handler = ioc.resolve<Handler>(Handler::class.simpleName!!, it.get<String>(handlerField))
                        handler.handle(handlersParamsObject)
                    } catch (e: Exception) {
                        Logger.getGlobal().warning("Can't execute map ${config.get<String>(nameField)}, handler ${map.indexOf(it)}. ${e.message}")
                        e.printStackTrace()
                        throw IllegalStateException("Can't execute map ${config.get<String>(nameField)}, handler ${map.indexOf(it)}. ${e.message}", e)
                    }
                } else {
                    try {
                        val mapToExecute = ioc.resolve<HandlersMap>(it.get<String>(mapField))
                        handlersParamsObject.get<IObject<Any>>(contextObjectField)
                                .addAll(
                                        mapToExecute
                                                .execute(
                                                        handlersParamsObject.get(
                                                                contextObjectField
                                                        )
                                                ).get()
                                )
                    } catch (e: Exception) {
                        Logger.getGlobal().warning("Can't execute map ${config.get<String>(nameField)}, submap ${map.indexOf(it)}. ${e.message}")
                        e.printStackTrace()
                        throw IllegalStateException("Can't execute map ${config.get<String>(nameField)}, handler ${map.indexOf(it)}. ${e.message}", e)
                    }
                }
            }
            handlersParamsObject.get(resultObjectField)
        })
    }
}
