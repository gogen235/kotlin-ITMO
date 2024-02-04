package airline.servies

import kotlinx.datetime.Instant

data class DelayFlight(val flightId: String, val departureTime: Instant, val actualDepartureTime: Instant) : Action
