package chatbot.dsl

import chatbot.api.Keyboard
import chatbot.api.MessageId

interface InterfaceMessageBuilder {

    var text: String
    var replyTo: MessageId?
    var keyboard: Keyboard?
    fun removeKeyboard()
    fun withKeyboard(configure: InterfaceKeyboardBuilder.() -> Unit)
}
