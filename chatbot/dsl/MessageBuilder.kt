package chatbot.dsl

import chatbot.api.Keyboard
import chatbot.api.MessageId

internal class MessageBuilder : InterfaceMessageBuilder {
    override var text = ""
    override var replyTo: MessageId? = null
    override var keyboard: Keyboard? = null
    var isEmptyKeyboard = true

    override fun removeKeyboard() {
        keyboard = Keyboard.Remove
        isEmptyKeyboard = false
    }

    override fun withKeyboard(configure: InterfaceKeyboardBuilder.() -> Unit) {
        val builder = KeyboardBuilder()
        builder.configure()
        isEmptyKeyboard = builder.keyboard.isEmpty()
        builder.keyboard.forEach { if (it.isEmpty()) isEmptyKeyboard = true }
        keyboard = Keyboard.Markup(builder.oneTime, builder.keyboard)
    }
}
