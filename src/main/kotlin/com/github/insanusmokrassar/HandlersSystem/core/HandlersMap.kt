package com.github.insanusmokrassar.HandlersSystem.core

import com.github.insanusmokrassar.HandlersSystem.*
import com.github.insanusmokrassar.IOC.core.ResolveStrategyException
import com.github.insanusmokrassar.IOC.core.getOrCreateIOC
import com.github.insanusmokrassar.IObjectK.interfaces.IInputObject
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectK.interfaces.addAll
import com.github.insanusmokrassar.IObjectK.interfaces.has
import com.github.insanusmokrassar.IObjectK.realisations.SimpleIObject
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.logging.Logger

/**
 * @param systemConfigObject Receive as first param object with settings of system: IOC object and other params which can be neede by handlers.
 * @param config object which contains:
 * <pre>
 *     {
 *          "name": "ExampleHandlersMapName",//required
 *          "path": [// required, contain path of map
 *              {
 *                  "name": "Name of handler",// name of map or handler
 *                  "executeConfig": {//optionally, will added when will executing as context params
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
class HandlersMap(
        private val config: IInputObject<String, Any>,
        private val systemConfigObject: IObject<Any> = SimpleIObject()
) : Handler {
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

    override fun handle(requestParams: IObject<Any>, requestResult: (IObject<Any>) -> Unit) {
        executor.submit({
            val map = config.get<List<IObject<Any>>>(pathField)
            val handlersParamsObject = SimpleIObject()
            val result = SimpleIObject()
            handlersParamsObject.put(systemConfigObjectField, systemConfigObject)
            handlersParamsObject.put(
                    contextObjectField,
                    if (requestParams.has(contextObjectField)) {
                        val contextParams = requestParams.getContextIObject()
                        contextParams.addAll(executeConfig)
                        contextParams
                    } else {
                        SimpleIObject(executeConfig)
                    }
            )
            handlersParamsObject.put(
                    requestObjectField,
                    if (requestParams.has(requestObjectField)) {
                        requestParams.getRequestIObject()
                    } else {
                        requestParams
                    }
            )
            val ioc = getOrCreateIOC(config.get(IOCNameField))
            val syncObject = Object()
            map.forEach {
                var next = false
                if (it.has(executeConfigField)) {
                    requestParams.getContextIObject().addAll(
                            it.get<IObject<Any>>(
                                    executeConfigField
                            )
                    )
                }
                try {
                    val handler = try {
                        ioc.resolve<Handler>(Handler::class.simpleName!!, it.get<String>(nameField))
                    } catch (e: ResolveStrategyException) {
                        ioc.resolve<Handler>(HandlersMap::class.simpleName!!, it.get<String>(nameField))
                    }
                    handler.handle(
                            handlersParamsObject,
                            {
                                synchronized(syncObject, {
                                    result.addAll(it)
                                    next = true
                                    syncObject.notify()
                                })
                            }
                    )
                    synchronized(syncObject, {
                        while (!next) {
                            syncObject.wait()
                        }
                    })
                } catch (e: Exception) {
                    Logger.getGlobal().warning("Can't execute map ${config.get<String>(nameField)}, handler ${map.indexOf(it)}. ${e.message}")
                    e.printStackTrace()
                    throw IllegalStateException("Can't execute map ${config.get<String>(nameField)}, handler ${map.indexOf(it)}. ${e.message}", e)
                }
            }
            requestResult(result)
        })
    }
}

fun IInputObject<String, Any>.getSystemIObject(): IObject<Any> {
    return get(systemConfigObjectField)
}

fun IInputObject<String, Any>.getContextIObject(): IObject<Any> {
    return get(contextObjectField)
}

fun IInputObject<String, Any>.getRequestIObject(): IObject<Any> {
    return get(requestObjectField)
}
