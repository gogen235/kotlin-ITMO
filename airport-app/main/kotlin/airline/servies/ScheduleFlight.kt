package airline.servies

import airline.api.Plane
import kotlinx.datetime.Instant

data class ScheduleFlight(val flightId: String, val departureTime: Instant, val plane: Plane) : Action
