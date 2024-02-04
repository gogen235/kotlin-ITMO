package chatbot.dsl

import chatbot.api.ChatContext
import chatbot.api.ChatContextsManager
import chatbot.api.ChatId

internal class DefaultChatContextManager : ChatContextsManager {
    private val data = mutableMapOf<Long, ChatContext?>()

    override fun getContext(chatId: ChatId): ChatContext? {
        return (chatId as? ChatId.Id)?.run { data[id] }
    }

    override fun setContext(chatId: ChatId, newState: ChatContext?) {
        (chatId as? ChatId.Id)?.run {
            data[id] = newState
        }
    }
}
