package com.github.insanusmokrassar.HandlersSystem.core

import com.github.insanusmokrassar.HandlersSystem.*
import com.github.insanusmokrassar.IOC.core.getOrCreateIOC
import com.github.insanusmokrassar.IObjectK.interfaces.IInputObject
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectK.interfaces.addAll
import com.github.insanusmokrassar.IObjectK.interfaces.has
import com.github.insanusmokrassar.IObjectK.realisations.SimpleIObject
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.logging.Logger

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
 *                  "params": {//optionally, will added when will executing as context params
 *                      ...
 *                  }
 *              }
 *          ],
 *          "executeConfig": {//optionally, will added when will executing as context params
 *              ...
 *          },
 *          "threadsGroupName":, "common"//optionally, will using to get executor service by executor service strategy with the name
 *          "IOCName": "name of ioc"
 *     }
 * </pre>
 */
class HandlersMap(private val config: IInputObject<String, Any>, private val systemConfigObject: IObject<Any> = SimpleIObject()) {

    private val executor: ExecutorService =
            if (config.has(threadsGroupNameField)) {
                try {
                    getOrCreateIOC(
                            config.get(IOCNameField)
                    ).resolve<ExecutorService>(
                            ExecutorService::class.simpleName!!,
                            threadsGroupNameField
                    )
                } catch (e: Exception) {
                    Executors.newSingleThreadExecutor()
                }
            } else {
                Executors.newSingleThreadExecutor()
            }

    private val executeConfig: IObject<Any> = {
        if (config.keys().contains(executeConfigField)) {
            config.get(executeConfigField)
        } else {
            SimpleIObject()
        }
    }()

    fun execute(requestParams: IObject<Any>): Future<IObject<Any>> {
        return executor.submit(Callable<IObject<Any>>{
            val map = config.get<List<IObject<Any>>>(pathField)
            val handlersParamsObject = SimpleIObject()
            handlersParamsObject.put(systemConfigObjectField, systemConfigObject)
            handlersParamsObject.put(contextObjectField, SimpleIObject(executeConfig))
            handlersParamsObject.put(requestObjectField, requestParams)
            handlersParamsObject.put(resultObjectField, SimpleIObject())
            val ioc = getOrCreateIOC(config.get(IOCNameField))
            map.forEach {
                if (it.has(executeConfigField)) {
                    handlersParamsObject.put(
                            contextObjectField,
                            it.get<IObject<Any>>(executeConfigField).addAll(
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
                        val mapToExecute = ioc.resolve<HandlersMap>(HandlersMap::class.simpleName!!, it.get(mapField))
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
