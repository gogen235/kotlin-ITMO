package chatbot.dsl

interface InterfaceRowBuilder {
    fun button(text: String)
    operator fun String.unaryMinus()
}
