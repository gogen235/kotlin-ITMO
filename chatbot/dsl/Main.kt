package chatbot.dsl

import chatbot.api.*

typealias InterfaceMessageBuilderFunc = InterfaceMessageBuilder.() -> Unit
fun chatBot(client: Client, configure: InterfaceBotBuilder.() -> Unit): ChatBot {
    val builder = BotBuilder()
    builder.configure()
    return object : ChatBot {
        override fun processMessages(message: Message) {
            val context = builder.contextManager.getContext(message.chatId)
            val behaviour = builder.behaviour.behaviour
            for (item in behaviour) {
                if (item.predicate(this, message)) {
                    item.action(
                        MessageProcessorContext(
                            message,
                            client,
                            context,
                        ) { builder.contextManager.setContext(message.chatId, it) },
                    )
                    break
                }
            }
        }

        override var logLevel: LogLevel = builder.logLevel
    }
}

fun <T : ChatContext?> MessageProcessorContext<T>.sendMessage(chatId: ChatId, configure: InterfaceMessageBuilderFunc) {
    val builder = MessageBuilder()
    builder.configure()
    if (builder.isEmptyKeyboard && builder.text == "") return
    client.sendMessage(chatId, builder.text, builder.keyboard, builder.replyTo)
}
