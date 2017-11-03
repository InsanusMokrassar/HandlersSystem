# This is changelog for HandlersSystem project

## v1.1

Now [Handlers](src/main/kotlin/com/github/insanusmokrassar/HandlersSystem/core/Handler.kt) must not get some result
callback and must add results of calculation in result object using
[getResultIObject](src/main/kotlin/com/github/insanusmokrassar/HandlersSystem/core/HandlersMap.kt#143) fun. Besides, in
[HandlersMap](src/main/kotlin/com/github/insanusmokrassar/HandlersSystem/core/HandlersMap.kt) was added method
[handleAsync](src/main/kotlin/com/github/insanusmokrassar/HandlersSystem/core/HandlersMaps.kt#122) to handle message
async with current thread.  