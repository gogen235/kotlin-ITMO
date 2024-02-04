package chatbot.dsl

import chatbot.api.Keyboard

internal class KeyboardBuilder : InterfaceKeyboardBuilder {
    override var oneTime = false
    override var keyboard: MutableList<MutableList<Keyboard.Button>> = mutableListOf()
    override fun row(configure: InterfaceRowBuilder.() -> Unit) {
        val builder = RowBuilder()
        configure(builder)
        keyboard.add(builder.row)
    }
}
