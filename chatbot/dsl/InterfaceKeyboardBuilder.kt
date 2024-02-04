package chatbot.dsl

import chatbot.api.Keyboard

interface InterfaceKeyboardBuilder {
    var oneTime: Boolean
    var keyboard: MutableList<MutableList<Keyboard.Button>>
    fun row(configure: InterfaceRowBuilder.() -> Unit)
}
