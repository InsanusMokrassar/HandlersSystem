package com.github.insanusmokrassar.HandlersSystem.core

import com.github.insanusmokrassar.HandlersSystem.*
import com.github.insanusmokrassar.IOC.core.ResolveStrategyException
import com.github.insanusmokrassar.IOC.core.getOrCreateIOC
import com.github.insanusmokrassar.IObjectK.interfaces.IInputObject
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectK.interfaces.has
import com.github.insanusmokrassar.IObjectK.realisations.SimpleIObject
import com.github.insanusmokrassar.IObjectK.utils.plus
import com.github.insanusmokrassar.IObjectK.utils.plusAssign
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.logging.Logger

private val commonHandlersMapExecutor = Executors.newSingleThreadExecutor()

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
                            config[IOCNameField]
                    ).resolve<ExecutorService>(
                            ExecutorService::class.simpleName!!,
                            config[threadsGroupNameField]
                    )
                } catch (e: Exception) {
                    commonHandlersMapExecutor
                }
            } else {
                commonHandlersMapExecutor
            }

    private val executeConfig: IObject<Any> =
            if (config.keys().contains(executeConfigField)) {
                config[executeConfigField]
            } else {
                SimpleIObject()
            }

    override fun handle(requestParams: IObject<Any>) {
        val map = config.get<List<IObject<Any>>>(pathField)
        val handlersParamsObject = SimpleIObject()
        handlersParamsObject[systemConfigObjectField] = systemConfigObject
        handlersParamsObject[contextObjectField] = if (requestParams.has(contextObjectField)) {
            requestParams.getContextIObject() + executeConfig
        } else {
            SimpleIObject(executeConfig)
        }
        handlersParamsObject[requestObjectField] = if (requestParams.has(requestObjectField)) {
            requestParams.getRequestIObject()
        } else {
            requestParams
        }
        handlersParamsObject[resultObjectField] = if (requestParams.has(resultObjectField)) {
            requestParams.getResultIObject()
        } else {
            SimpleIObject()
        }
        val ioc = getOrCreateIOC(config[IOCNameField])
        map.forEach {
            if (it.has(executeConfigField)) {
                requestParams.getContextIObject() += it.get<IObject<Any>>(executeConfigField)

            }
            try {
                val handler = try {
                    ioc.resolve<Handler>(Handler::class.simpleName!!, it.get<String>(nameField))
                } catch (e: ResolveStrategyException) {
                    ioc.resolve<Handler>(HandlersMap::class.simpleName!!, it.get<String>(nameField))
                }
                handler.handle(handlersParamsObject)
            } catch (e: Exception) {
                Logger.getGlobal().warning("Can't execute map ${config.get<String>(nameField)}, handler ${map.indexOf(it)}. ${e.message}")
                e.printStackTrace()
                throw IllegalStateException("Can't execute map ${config.get<String>(nameField)}, handler ${map.indexOf(it)}. ${e.message}", e)
            }
        }
    }

    fun handleAsync(
            requestParams: IObject<Any>,
            executor: ExecutorService = this.executor
    ) : Future<IObject<Any>> = executor.submit (Callable<IObject<Any>>{
        handle(requestParams)
        requestParams.getResultIObject()
    })
}

fun IInputObject<String, Any>.getSystemIObject(): IInputObject<String, Any> {
    return get(systemConfigObjectField)
}

fun IInputObject<String, Any>.getContextIObject(): IObject<Any> {
    return get(contextObjectField)
}

fun IInputObject<String, Any>.getRequestIObject(): IInputObject<String, Any> {
    return get(requestObjectField)
}

fun IInputObject<String, Any>.getResultIObject(): IObject<Any> {
    return get(resultObjectField)
}
