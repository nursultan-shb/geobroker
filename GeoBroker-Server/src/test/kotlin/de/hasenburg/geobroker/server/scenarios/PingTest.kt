package de.hasenburg.geobroker.server.scenarios

import de.hasenburg.geobroker.client.main.SimpleClient
import de.hasenburg.geobroker.commons.communication.ZMQProcessManager
import de.hasenburg.geobroker.commons.model.message.Payload.*
import de.hasenburg.geobroker.commons.model.message.ReasonCode
import de.hasenburg.geobroker.commons.model.spatial.Location
import de.hasenburg.geobroker.commons.sleepNoLog
import de.hasenburg.geobroker.server.main.Configuration
import de.hasenburg.geobroker.server.main.server.SingleGeoBrokerServerLogic
import kg.shabykeev.loadbalancer.server.LoadBalancerLogic
import org.apache.logging.log4j.LogManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kg.shabykeev.loadbalancer.commons.server.Configuration as LbConfiguration

class PingTest {

    private val logger = LogManager.getLogger()
    private lateinit var serverLogic: SingleGeoBrokerServerLogic
    private lateinit var loadBalancerLogic: LoadBalancerLogic
    private lateinit var clientProcessManager: ZMQProcessManager
    private val receiveTimeout = 3000

    @Before
    fun setUp() {
        logger.info("Running test setUp")

        serverLogic = SingleGeoBrokerServerLogic()
        serverLogic.loadConfiguration(Configuration())
        serverLogic.initializeFields()
        serverLogic.startServer()

        loadBalancerLogic = LoadBalancerLogic()
        loadBalancerLogic.loadConfiguration(LbConfiguration.readInternalConfiguration("lb_configuration.toml"))
        loadBalancerLogic.initializeFields()
        loadBalancerLogic.startServer()

        clientProcessManager = ZMQProcessManager()

        //give a time to start LoadBalancer environment
        sleepNoLog(15000, 0)
    }

    @After
    fun tearDown() {
        logger.info("Running test tearDown.")
        clientProcessManager.tearDown(2000)
        serverLogic.cleanUp()
        loadBalancerLogic.cleanUp()
    }

    @Test
    fun compareSerial() {
        val time1 = System.nanoTime().toDouble()
        // connect, ping, and disconnect
        val client = SimpleClient("localhost", 7225, clientProcessManager)
        client.send(CONNECTPayload(Location.random()))
        for (i in 0..99) {
            client.send(PINGREQPayload(Location.random()))
        }

        client.send(DISCONNECTPayload(ReasonCode.NormalDisconnection))

        for (i in 0..100) {
            val payload = client.receiveWithTimeout(receiveTimeout)
            if (i == 0) {
                assertTrue(payload is CONNACKPayload)
            } else {
                if (payload is PINGRESPPayload) {
                    assertEquals(ReasonCode.LocationUpdated, payload.reasonCode)
                } else {
                    fail("Wrong payload, received $payload")
                }
            }
        }
        val time2 = System.nanoTime().toDouble()
        logger.info("Messages took {}s", (time2 - time1) / 1000000000)
    }

    @Test
    fun testPingWhileConnected() {
        // connect, ping, and disconnect
        val client = SimpleClient("localhost", 7225, clientProcessManager)
        client.send(CONNECTPayload(Location.random()))
        for (i in 0..9) {
            client.send(PINGREQPayload(Location.random()))
            sleepNoLog(100, 0)
        }

        client.send(DISCONNECTPayload(ReasonCode.NormalDisconnection))

        // check dealer messages
        for (i in 0..10) {
            val payload = client.receiveWithTimeout(receiveTimeout)

            if (i == 0) {
                assertTrue(payload is CONNACKPayload)
            } else {
                if (payload is PINGRESPPayload) {
                    assertEquals(ReasonCode.LocationUpdated, payload.reasonCode)
                } else {
                    fail("Wrong payload, received $payload")
                }
            }
        }
    }

    @Test
    fun testPingWhileNotConnected() {
        val client = SimpleClient("localhost", 7225, clientProcessManager)

        client.send(PINGREQPayload(Location.random()))

        val payload = client.receiveWithTimeout(receiveTimeout)

        if (payload is PINGRESPPayload) {
            assertEquals(ReasonCode.NotConnected, payload.reasonCode)
        } else {
            fail("Wrong payload, received $payload")
        }
    }

}
