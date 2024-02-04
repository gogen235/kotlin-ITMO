package chatbot.dsl

import chatbot.api.ChatBot
import chatbot.api.ChatContext
import chatbot.api.Message

class Action<T : ChatContext?>(val predicate: ChatBot.(message: Message) -> Boolean, val action: MessageProcessor<T>)

@TagDsl
open class BehaviourBuilder<T : ChatContext?> {
    var behaviour: MutableList<Action<T>> = mutableListOf()

    fun onCommand(command: String, doIfCommand: MessageProcessor<T>) {
        behaviour.add(Action({ message -> message.text.startsWith("/$command") }, doIfCommand))
    }

    fun onMessage(predicate: ChatBot.(Message) -> Boolean, doIfPredicate: MessageProcessor<T>) {
        behaviour.add(Action(predicate, doIfPredicate))
    }

    fun onMessagePrefix(preffix: String, doIfPreffix: MessageProcessor<T>) {
        behaviour.add(Action({ message -> message.text.startsWith(preffix) }, doIfPreffix))
    }

    fun onMessageContains(text: String, doIfText: MessageProcessor<T>) {
        behaviour.add(Action({ message -> message.text.contains(text) }, doIfText))
    }

    fun onMessage(messageTextExactly: String, doIfMessageTextExactly: MessageProcessor<T>) {
        behaviour.add(Action({ message -> message.text == messageTextExactly }, doIfMessageTextExactly))
    }

    fun onMessage(doIfNothing: MessageProcessor<T>) {
        behaviour.add(Action({ true }, doIfNothing))
    }
}
