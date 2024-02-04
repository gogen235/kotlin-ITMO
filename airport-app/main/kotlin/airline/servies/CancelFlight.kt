package airline.servies

import kotlinx.datetime.Instant

data class CancelFlight(val flightId: String, val departureTime: Instant) : Action
