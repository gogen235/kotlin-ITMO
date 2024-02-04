package chatbot.dsl

import chatbot.api.ChatContextsManager
import chatbot.api.LogLevel
@DslMarker
annotation class TagDsl

@TagDsl
interface InterfaceBotBuilder {
    fun use(logLevel: LogLevel)
    operator fun LogLevel.unaryPlus()
    fun behaviour(configure: DefaultBehaviourBuilder.() -> Unit)
    fun use(contextManager: ChatContextsManager)
}
