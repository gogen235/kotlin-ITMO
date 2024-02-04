package airline.servies

import kotlinx.coroutines.channels.Channel

class BufferedEmailService(private val emailService: EmailService) : EmailService {
    data class Email(val to: String, val text: String)

    private val buffer = Channel<Email>(capacity = Channel.BUFFERED)
    override suspend fun send(to: String, text: String) {
        buffer.send(Email(to, text))
    }
    suspend fun sendEmails() {
        for (email in buffer) {
            emailService.send(email.to, email.text)
        }
    }
}
