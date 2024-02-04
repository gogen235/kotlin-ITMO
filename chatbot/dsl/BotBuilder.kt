package chatbot.dsl

import chatbot.api.ChatContextsManager
import chatbot.api.LogLevel

internal class BotBuilder : InterfaceBotBuilder {
    var logLevel: LogLevel = LogLevel.ERROR
    var behaviour: DefaultBehaviourBuilder = DefaultBehaviourBuilder()
    var contextManager: ChatContextsManager = DefaultChatContextManager()
    override fun use(logLevel: LogLevel) {
        this.logLevel = logLevel
    }

    override operator fun LogLevel.unaryPlus() {
        logLevel = this
    }

    override fun behaviour(configure: DefaultBehaviourBuilder.() -> Unit) {
        val builder = DefaultBehaviourBuilder()
        builder.configure()
        builder.contextManager = contextManager
        behaviour = builder
    }

    override fun use(contextManager: ChatContextsManager) {
        this.contextManager = contextManager
    }
}
