package airline.servies

import kotlinx.datetime.Instant

data class SetCheckInNumber(val flightId: String, val departureTime: Instant, val checkInNumber: String) : Action
