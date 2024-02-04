package airline

import airline.api.AirlineConfig
import airline.api.Plane
import airline.servies.EmailService
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AirportTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testServices() {
        val emailService = InChannelEmailService()
        val config = AirlineConfig(
            audioAlertsInterval = 500.milliseconds,
            displayUpdateInterval = 100.milliseconds,
        )
        val airlineApplication = AirlineApplication(config, emailService)
        val plane = listOf(
            Plane("A312", setOf("1A", "1B", "2A", "2B")),
            Plane("B313", setOf("1A", "1B", "2A", "2B", "3A", "3B")),
            Plane("C314", setOf("1A", "1B", "1C", "2A", "2B", "2C")),
            Plane("B315", setOf("1A", "1B", "2A", "2B", "3A", "3B")),
            Plane("C316", setOf("1A", "1B", "1C", "2A", "2B", "2C")),
        )

        val flightId = listOf("111", "112", "113", "114", "115")

        val flightTime = listOf(
            Clock.System.now() + 1.hours,
            Clock.System.now() + 2.hours,
            Clock.System.now() + 3.hours,
            Clock.System.now() + 4.hours,
            Clock.System.now() + 5.hours,
        )
        testAndCancel {
            launch { airlineApplication.run() }
            sleep()
            val booking = airlineApplication.bookingService
            val management = airlineApplication.managementService
            val display = airlineApplication.airportInformationDisplay(this)
            for (i in 0..4) {
                management.scheduleFlight(flightId[i], flightTime[i], plane[i])
            }
            sleep()

            Assertions.assertEquals(5, display.value.departing.size)
            for (i in 0..4) {
                Assertions.assertEquals(flightId[i], display.value.departing[i].flightId)
            }

            // buy tickets
            val passenger = listOf("Andrew Stankevich", "Georgy Korneev", "Konstantin Bats", "Grigoriy Khlytin")
            val email = listOf("dm.itmo.ct@gmail.ru", "paradigms@gmail.ru", "kbats@itmo.ru", "kgrigoriy@gmail.ru")

            booking.buyTicket(flightId[0], flightTime[0], "2A", "1", passenger[0], email[0])
            booking.buyTicket(flightId[0], flightTime[0], "2B", "2", passenger[1], email[1])
            booking.buyTicket(flightId[1], flightTime[1], "3A", "3", passenger[2], email[2])
            booking.buyTicket(flightId[1], flightTime[1], "3B", "4", passenger[3], email[3])
            sleep()

            Assertions.assertEquals(5, display.value.departing.size)

            Assertions.assertEquals(4, booking.flightSchedule.size)
            for (i in 0..3) {
                print(booking.flightSchedule[i].flightId)
                Assertions.assertEquals(flightId[i + 1], booking.flightSchedule[i].flightId)
            }

            Assertions.assertEquals(setOf("1A", "1B", "2A", "2B"), booking.freeSeats(flightId[0], flightTime[0]))
            Assertions.assertEquals(setOf("1A", "1B", "2A", "2B"), booking.freeSeats(flightId[1], flightTime[1]))

            for (i in 0..1) {
                val (email1, text1) = emailService.messages.receive()
                Assertions.assertEquals(email[i], email1)
                Assertions.assertTrue("can't" in text1)
            }
            for (i in 2..3) {
                val (email1, text1) = emailService.messages.receive()
                Assertions.assertEquals(email[i], email1)
                Assertions.assertTrue("successfully" in text1)
            }

            // buy illegal ticket
            booking.buyTicket("111", flightTime[0], "2A", "5", "Kucheruk Ekaterina Arkadievna", "linalg@mail.ru")
            sleep()

            val (email2, text2) = emailService.messages.receive()
            Assertions.assertEquals("linalg@mail.ru", email2)
            Assertions.assertTrue("can't" in text2)

            // more passengers

            val passengers = listOf("Bob1", "Bob2", "Bob3", "Bob4", "Bob5", "Bob6")
            val emails = listOf(
                "bob1@mail.ru",
                "bob2@mail.ru",
                "bob3@mail.ru",
                "bob4@mail.ru",
                "bob5@mail.ru",
                "bob6@mail.ru",
            )
            val seats = listOf("1A", "1B", "1C", "2A", "2B", "2C")

            for (i in 0..5) {
                booking.buyTicket(flightId[2], flightTime[2], seats[i], i.toString(), passengers[i], emails[i])
            }
            sleep()

            for (i in 0..5) {
                val (email1, text1) = emailService.messages.receive()
                Assertions.assertEquals(emails[i], email1)
                Assertions.assertTrue("successfully" in text1)
            }
            Assertions.assertEquals(booking.freeSeats(flightId[2], flightTime[2]), emptySet<String>())

            // delay
            for (i in 0..2) {
                management.delayFlight(flightId[2], flightTime[2], flightTime[2] + (6 + i).hours)
                sleep()

                Assertions.assertEquals(5, display.value.departing.size)
                Assertions.assertEquals(flightId[2], display.value.departing[2].flightId)
                Assertions.assertEquals(flightTime[2], display.value.departing[2].departureTime)
                Assertions.assertEquals(flightTime[2] + (6 + i).hours, display.value.departing[2].actualDepartureTime)

                for (j in 0..5) {
                    val (email1, text1) = emailService.messages.receive()
                    Assertions.assertEquals(emails[j], email1)
                    Assertions.assertTrue("delayed" in text1)
                }
                Assertions.assertTrue(emailService.messages.isEmpty)
            }

            // check-in
            for (i in 0..2) {
                management.setCheckInNumber(flightId[2], flightTime[2], "c$i")
                sleep()

                Assertions.assertEquals(5, display.value.departing.size)
                Assertions.assertEquals(flightId[2], display.value.departing[2].flightId)
                Assertions.assertEquals("c$i", display.value.departing[2].checkInNumber)

                for (j in 0..5) {
                    val (email1, text1) = emailService.messages.receive()
                    Assertions.assertEquals(emails[j], email1)
                    Assertions.assertTrue("check-in" in text1)
                }
                Assertions.assertTrue(emailService.messages.isEmpty)
            }

            // gate
            for (i in 0..2) {
                management.setGateNumber(flightId[2], flightTime[2], "g$i")
                sleep()

                Assertions.assertEquals(5, display.value.departing.size)
                Assertions.assertEquals(flightId[2], display.value.departing[2].flightId)
                Assertions.assertEquals("g$i", display.value.departing[2].gateNumber)

                for (j in 0..5) {
                    val (email1, text1) = emailService.messages.receive()
                    Assertions.assertEquals(emails[j], email1)
                    Assertions.assertTrue("gate" in text1)
                }
                Assertions.assertTrue(emailService.messages.isEmpty)
            }

            // cancel
            management.cancelFlight(flightId[2], flightTime[2])
            sleep()

            Assertions.assertEquals(5, display.value.departing.size)
            Assertions.assertEquals(flightId[2], display.value.departing[2].flightId)
            Assertions.assertTrue(display.value.departing[2].isCancelled)

            for (j in 0..5) {
                val (email1, text1) = emailService.messages.receive()
                Assertions.assertEquals(emails[j], email1)
                Assertions.assertTrue("canceled" in text1)
            }
            Assertions.assertTrue(emailService.messages.isEmpty)
        }
    }

// To test this, you need to reduce time intervals from 3.minutes to 500.milliseconds
//    @Test
//    fun testAudioAlerts() {
//        val emailService = InChannelEmailService()
//        val config = AirlineConfig(
//            audioAlertsInterval = 500.milliseconds,
//            displayUpdateInterval = 100.milliseconds,
//            registrationOpeningTime = 3.seconds,
//            registrationClosingTime = 1.seconds,
//            boardingOpeningTime = 2.seconds,
//            boardingClosingTime = 1.seconds,
//        )
//        val airlineApplication = AirlineApplication(config, emailService)
//        val plane = Plane("A312", setOf())
//
//        val flightId1 = "111"
//        val flightId2 = "112"
//        val flightId3 = "113"
//        val flightId4 = "114"
//
//        val flightTime1 = Clock.System.now() + 4.seconds
//        val flightTime2 = Clock.System.now() + 5.seconds
//        val flightTime3 = Clock.System.now() + 6.seconds
//        val flightTime4 = Clock.System.now() + 7.seconds
//        testAndCancel {
//            launch { airlineApplication.run() }
//            sleep()
//            val management = airlineApplication.managementService
//            val display = airlineApplication.airportInformationDisplay(this)
//            val alerts = airlineApplication.airportAudioAlerts
//            management.scheduleFlight(flightId1, flightTime1, plane)
//            management.scheduleFlight(flightId2, flightTime2, plane)
//            management.scheduleFlight(flightId3, flightTime3, plane)
//            management.scheduleFlight(flightId4, flightTime4, plane)
//            management.delayFlight(flightId3, flightTime3, flightTime3 + 3.seconds)
//            sleep()
//
//            Assertions.assertEquals(4, display.value.departing.size)
//            Assertions.assertEquals(flightId1, display.value.departing[0].flightId)
//            Assertions.assertEquals(flightId2, display.value.departing[1].flightId)
//            Assertions.assertEquals(flightId3, display.value.departing[2].flightId)
//            Assertions.assertEquals(flightId4, display.value.departing[3].flightId)
//            Assertions.assertEquals(flightTime3, display.value.departing[2].departureTime)
//            Assertions.assertEquals(flightTime3 + 3.seconds, display.value.departing[2].actualDepartureTime)
//
//            val alertsList = mutableListOf<AudioAlerts>()
//            withTimeoutOrNull<Any>(9.seconds) {
//                alerts.collect {
//                    alertsList.add(it)
//                }
//            }
//
//            Assertions.assertTrue(alertsList[0] is AudioAlerts.RegistrationOpen)
//            Assertions.assertEquals(flightId1, (alertsList[0] as AudioAlerts.RegistrationOpen).flightNumber)
//            Assertions.assertTrue(alertsList[1] is AudioAlerts.BoardingOpened)
//            Assertions.assertEquals(flightId1, (alertsList[1] as AudioAlerts.BoardingOpened).flightNumber)
//            Assertions.assertTrue(alertsList[2] is AudioAlerts.RegistrationOpen)
//            Assertions.assertEquals(flightId2, (alertsList[2] as AudioAlerts.RegistrationOpen).flightNumber)
//            Assertions.assertTrue(alertsList[3] is AudioAlerts.RegistrationClosing)
//            Assertions.assertEquals(flightId1, (alertsList[3] as AudioAlerts.RegistrationClosing).flightNumber)
//            Assertions.assertTrue(alertsList[4] is AudioAlerts.BoardingClosing)
//            Assertions.assertEquals(flightId1, (alertsList[4] as AudioAlerts.BoardingClosing).flightNumber)
//            Assertions.assertTrue(alertsList[5] is AudioAlerts.BoardingOpened)
//            Assertions.assertEquals(flightId2, (alertsList[5] as AudioAlerts.BoardingOpened).flightNumber)
//            Assertions.assertTrue(alertsList[6] is AudioAlerts.RegistrationClosing)
//            Assertions.assertEquals(flightId2, (alertsList[6] as AudioAlerts.RegistrationClosing).flightNumber)
//            Assertions.assertTrue(alertsList[7] is AudioAlerts.BoardingClosing)
//            Assertions.assertEquals(flightId2, (alertsList[7] as AudioAlerts.BoardingClosing).flightNumber)
//            Assertions.assertTrue(alertsList[8] is AudioAlerts.RegistrationOpen)
//            Assertions.assertEquals(flightId4, (alertsList[8] as AudioAlerts.RegistrationOpen).flightNumber)
//            Assertions.assertTrue(alertsList[9] is AudioAlerts.BoardingOpened)
//            Assertions.assertEquals(flightId4, (alertsList[9] as AudioAlerts.BoardingOpened).flightNumber)
//            Assertions.assertTrue(alertsList[10] is AudioAlerts.RegistrationClosing)
//            Assertions.assertEquals(flightId4, (alertsList[10] as AudioAlerts.RegistrationClosing).flightNumber)
//            Assertions.assertTrue(alertsList[11] is AudioAlerts.BoardingClosing)
//            Assertions.assertEquals(flightId4, (alertsList[11] as AudioAlerts.BoardingClosing).flightNumber)
//            Assertions.assertTrue(alertsList[12] is AudioAlerts.RegistrationOpen)
//            Assertions.assertEquals(flightId3, (alertsList[12] as AudioAlerts.RegistrationOpen).flightNumber)
//            Assertions.assertTrue(alertsList[13] is AudioAlerts.BoardingOpened)
//            Assertions.assertEquals(flightId3, (alertsList[13] as AudioAlerts.BoardingOpened).flightNumber)
//            Assertions.assertTrue(alertsList[14] is AudioAlerts.RegistrationClosing)
//            Assertions.assertEquals(flightId3, (alertsList[14] as AudioAlerts.RegistrationClosing).flightNumber)
//            Assertions.assertTrue(alertsList[15] is AudioAlerts.BoardingClosing)
//            Assertions.assertEquals(flightId3, (alertsList[15] as AudioAlerts.BoardingClosing).flightNumber)
//        }
//    }

    private fun testAndCancel(block: suspend CoroutineScope.() -> Unit) {
        try {
            runBlocking {
                block()
                cancel()
            }
        } catch (ignore: CancellationException) {
        }
    }

    private suspend fun sleep() {
        delay(150.milliseconds)
    }

    private class InChannelEmailService : EmailService {
        val messages = Channel<Pair<String, String>>()

        override suspend fun send(to: String, text: String) {
            messages.send(to to text)
        }
    }
}
