package chatbot.dsl

import chatbot.api.ChatBot
import chatbot.api.ChatContext
import chatbot.api.ChatContextsManager
import chatbot.api.Message

@TagDsl
class DefaultBehaviourBuilder : BehaviourBuilder<ChatContext?>() {
    var contextManager: ChatContextsManager = DefaultChatContextManager()

    infix fun <T : ChatContext> T.into(configure: BehaviourBuilder<T>.() -> Unit) {
        val builder = BehaviourBuilder<T>()
        builder.configure()
        for (item in builder.behaviour) {
            val predicate: ChatBot.(message: Message) -> Boolean =
                { massage -> item.predicate(this, massage) && contextManager.getContext(massage.chatId) == this@into }
            val action = item.action as MessageProcessor<ChatContext?>
            behaviour.add(Action(predicate, action))
        }
    }

    inline fun <reified T : ChatContext> into(configure: BehaviourBuilder<T>.() -> Unit) {
        val builder = BehaviourBuilder<T>()
        builder.configure()
        for (item in builder.behaviour) {
            val predicate: ChatBot.(message: Message) -> Boolean =
                { massage -> item.predicate(this, massage) && contextManager.getContext(massage.chatId) is T }
            val action = item.action as MessageProcessor<ChatContext?>
            behaviour.add(Action(predicate, action))
        }
    }
}
