package airline

import airline.api.*
import airline.servies.*
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class AirlineApplication(private val config: AirlineConfig, private val emailService: EmailService) {
    val bookingService: BookingServices = object : BookingServices {
        override val flightSchedule: List<FlightInfo>
            get() = flightFlow.value.filter {
                it.tickers.size != it.plane.seats.size &&
                    !it.isCancelled &&
                    Clock.System.now() < it.actualDepartureTime - config.ticketSaleEndTime
            }.map { it.toFlightInfo() }

        override fun freeSeats(flightId: String, departureTime: Instant): Set<String> {
            val currentFlight = getFlight(flightId, departureTime)
            val ticketPlaces = currentFlight.tickers.values.map { it.seatNo }
            return currentFlight.plane.seats.filter { !ticketPlaces.contains(it) }.toSet()
        }

        override suspend fun buyTicket(
            flightId: String,
            departureTime: Instant,
            seatNo: String,
            passengerId: String,
            passengerName: String,
            passengerEmail: String,
        ) {
            airportActions.emit(BuyTicket(flightId, departureTime, seatNo, passengerId, passengerName, passengerEmail))
        }

    }

    val managementService: AirlineManagementService = object : AirlineManagementService {
        override suspend fun scheduleFlight(flightId: String, departureTime: Instant, plane: Plane) {
            airportActions.emit(ScheduleFlight(flightId, departureTime, plane))
        }

        override suspend fun delayFlight(flightId: String, departureTime: Instant, actualDepartureTime: Instant) {
            airportActions.emit(DelayFlight(flightId, departureTime, actualDepartureTime))
        }

        override suspend fun cancelFlight(flightId: String, departureTime: Instant) {
            airportActions.emit(CancelFlight(flightId, departureTime))
        }

        override suspend fun setCheckInNumber(flightId: String, departureTime: Instant, checkInNumber: String) {
            airportActions.emit(SetCheckInNumber(flightId, departureTime, checkInNumber))
        }

        override suspend fun setGateNumber(flightId: String, departureTime: Instant, gateNumber: String) {
            airportActions.emit(SetGateNumber(flightId, departureTime, gateNumber))
        }

    }

    private val bufferedEmailService = BufferedEmailService(emailService)
    private val passengerNotificationService = PassengerNotificationService()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun airportInformationDisplay(coroutineScope: CoroutineScope): StateFlow<InformationDisplay> {
        return flightFlow.mapLatest {
            delay(config.displayUpdateInterval)
            InformationDisplay(it.map { x -> x.toFlightInfo() })
        }.stateIn(coroutineScope, SharingStarted.Eagerly, InformationDisplay(emptyList()))
    }

    private var flightFlow = MutableStateFlow<List<Flight>>(emptyList())
    private var airportActions = MutableSharedFlow<Action>()
    val airportAudioAlerts: Flow<AudioAlerts>
        get() = flow {
            while (true) {
                flightFlow.value.forEach {
                    if (!it.isCancelled) {
                        val timeBeforeDeparture = Clock.System.now() - it.actualDepartureTime
                        val registrationOpen = timeBeforeDeparture + config.registrationOpeningTime
                        val registrationClosing = timeBeforeDeparture + config.registrationClosingTime
                        val boardingOpened = timeBeforeDeparture + config.boardingOpeningTime
                        val boardingClosing = timeBeforeDeparture + config.boardingClosingTime
                        val checkInNumber = it.checkInNumber ?: "No information"
                        val gateNumber = it.gateNumber ?: "No information"
                        val begin = 0.minutes
                        val end = 3.minutes
                        if (registrationOpen > begin && registrationOpen <= end) {
                            emit(AudioAlerts.RegistrationOpen(it.flightId, checkInNumber))
                        }
                        if (registrationClosing >= -end && registrationClosing < begin) {
                            emit(AudioAlerts.RegistrationClosing(it.flightId, checkInNumber))
                        }
                        if (boardingOpened > begin && boardingOpened <= end) {
                            emit(AudioAlerts.BoardingOpened(it.flightId, gateNumber))
                        }
                        if (boardingClosing >= -end && boardingClosing < begin) {
                            emit(AudioAlerts.BoardingClosing(it.flightId, gateNumber))
                        }
                    }
                }
                delay(config.audioAlertsInterval)
            }
        }

    suspend fun run() {
        coroutineScope {
            launch {
                doServiceAction()
            }
            launch {
                bufferedEmailService.sendEmails()
            }
            launch {
                sendNotification()
            }
        }

    }

    private suspend fun doServiceAction() {
        airportActions.collect { msg ->
            when (msg) {
                is ScheduleFlight -> {
                    flightFlow.value += Flight(msg.flightId, msg.departureTime, plane = msg.plane)
                }

                is DelayFlight -> {
                    val flight = replaceFlight(
                        msg.flightId,
                        msg.departureTime,
                    ) { it.copy(actualDepartureTime = msg.actualDepartureTime) }
                    if (flight != null) {
                        passengerNotificationService.sendDelayFlight(flight, msg.actualDepartureTime)
                    }
                }

                is CancelFlight -> {
                    val flight = replaceFlight(
                        msg.flightId,
                        msg.departureTime,
                    ) { it.copy(isCancelled = true) }
                    if (flight != null) {
                        passengerNotificationService.sendCancelFlight(flight)
                    }
                }

                is SetCheckInNumber -> {
                    val flight = replaceFlight(
                        msg.flightId,
                        msg.departureTime,
                    ) { it.copy(checkInNumber = msg.checkInNumber) }
                    if (flight != null) {
                        passengerNotificationService.sendSetCheckInNumber(flight, msg.checkInNumber)
                    }
                }

                is SetGateNumber -> {
                    val flight = replaceFlight(
                        msg.flightId,
                        msg.departureTime,
                    ) { it.copy(gateNumber = msg.gateNumber) }
                    if (flight != null) {
                        passengerNotificationService.sendSetGateNumber(flight, msg.gateNumber)
                    }
                }

                is BuyTicket -> {
                    val flight = getFlight(msg.flightId, msg.departureTime)
                    if (!flight.isCancelled &&
                        Clock.System.now() < flight.actualDepartureTime - config.ticketSaleEndTime &&
                        msg.seatNo in bookingService.freeSeats(
                            msg.flightId,
                            msg.departureTime,
                        )
                    ) {
                        bufferedEmailService.send(
                            msg.passengerEmail,
                            "Dear, ${msg.passengerName}. " +
                                "You have successfully buy a ticket on seat ${msg.seatNo} on flight ${msg.flightId} " +
                                "with departure time ${msg.departureTime}.",
                        )
                        replaceFlight(
                            msg.flightId,
                            msg.departureTime,
                        ) {
                            it.copy(
                                tickers = it.tickers + Pair(
                                    msg.seatNo,
                                    Ticket(
                                        msg.flightId,
                                        msg.departureTime,
                                        msg.seatNo,
                                        msg.passengerId,
                                        msg.passengerName,
                                        msg.passengerEmail,
                                    ),
                                ),
                            )
                        }
                    } else {
                        bufferedEmailService.send(
                            msg.passengerEmail,
                            "Dear, ${msg.passengerName}. " +
                                "You can't buy ticket on seat ${msg.seatNo} on flight ${msg.flightId} " +
                                "with departure time ${msg.departureTime}.",
                        )
                    }
                }
            }
        }
    }

    private suspend fun sendNotification() {
        for (notification in passengerNotificationService.buffer) {
            notification.tickers.values.forEach {
                emailService.send(it.passengerEmail, notification.text(it))
            }
        }
    }

    private fun getFlight(flightId: String, departureTime: Instant): Flight {
        for (item in flightFlow.value) {
            if (item.flightId == flightId && item.departureTime == departureTime) return item
        }
        throw IllegalArgumentException("No such flight")
    }

    private fun isSameFlight(flight: Flight, flightId: String, departureTime: Instant): Boolean {
        return flight.flightId == flightId && flight.departureTime == departureTime
    }

    private fun replaceFlight(flightId: String, departureTime: Instant, copyFlight: (Flight) -> Flight): Flight? {
        var flight: Flight? = null
        flightFlow.value = flightFlow.value.map {
            if (isSameFlight(it, flightId, departureTime)) {
                flight = copyFlight(it)
                copyFlight(it)
            } else {
                it
            }
        }
        return flight
    }

    fun Flight.toFlightInfo(): FlightInfo {
        return FlightInfo(
            flightId,
            departureTime,
            isCancelled,
            actualDepartureTime,
            checkInNumber,
            gateNumber,
            plane,
        )
    }
}
