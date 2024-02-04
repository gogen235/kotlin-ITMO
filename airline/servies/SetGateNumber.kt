package airline.servies

import kotlinx.datetime.Instant

data class SetGateNumber(val flightId: String, val departureTime: Instant, val gateNumber: String) : Action
