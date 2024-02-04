package airline.servies

import airline.api.Flight
import airline.api.Ticket
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.Instant

class PassengerNotificationService {
    data class Notification(val tickers: Map<String, Ticket>, val text: (ticket: Ticket) -> String)

    val buffer = Channel<Notification>()
    suspend fun sendDelayFlight(flight: Flight, actualDepartureTime: Instant) {
        buffer.send(
            Notification(
                flight.tickers,
            ) { ticket ->
                "Dear, ${ticket.passengerName}. " +
                    "Unfortunately, your flight ${flight.flightId} delayed " +
                    "from ${flight.departureTime} to $actualDepartureTime."
            },
        )
    }

    suspend fun sendCancelFlight(flight: Flight) {
        buffer.send(
            Notification(
                flight.tickers,
            ) { ticket ->
                "Dear, ${ticket.passengerName}. " +
                    "Unfortunately, your flight ${flight.flightId} " +
                    "with departure time ${flight.departureTime} canceled."
            },
        )
    }

    suspend fun sendSetCheckInNumber(flight: Flight, checkInNumber: String) {
        buffer.send(
            Notification(
                flight.tickers,
            ) { ticket ->
                "Dear, ${ticket.passengerName}. " +
                    "Number of check-in on flight ${flight.flightId} changed to $checkInNumber."
            },
        )
    }

    suspend fun sendSetGateNumber(flight: Flight, gateNumber: String) {
        buffer.send(
            Notification(
                flight.tickers,
            ) { ticket ->
                "Dear, ${ticket.passengerName}. " +
                    "Number of gate on flight ${flight.flightId} changed to $gateNumber."
            },
        )
    }
}
