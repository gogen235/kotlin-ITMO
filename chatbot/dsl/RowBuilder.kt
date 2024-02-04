package chatbot.dsl

import chatbot.api.Keyboard

internal class RowBuilder : InterfaceRowBuilder {
    var row: MutableList<Keyboard.Button> = mutableListOf()
    override fun button(text: String) {
        row.add(Keyboard.Button(text))
    }

    override operator fun String.unaryMinus() {
        row.add(Keyboard.Button(this))
    }
}
