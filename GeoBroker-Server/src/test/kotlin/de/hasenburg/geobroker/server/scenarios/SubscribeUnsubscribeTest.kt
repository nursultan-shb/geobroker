package de.hasenburg.geobroker.server.scenarios

import de.hasenburg.geobroker.client.main.SimpleClient
import de.hasenburg.geobroker.commons.communication.ZMQProcessManager
import de.hasenburg.geobroker.commons.model.message.Payload.*
import de.hasenburg.geobroker.commons.model.message.ReasonCode
import de.hasenburg.geobroker.commons.model.message.Topic
import de.hasenburg.geobroker.commons.model.spatial.Geofence
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

class SubscribeUnsubscribeTest {

    private val logger = LogManager.getLogger()
    private lateinit var serverLogic: SingleGeoBrokerServerLogic
    private lateinit var loadBalancerLogic: LoadBalancerLogic
    private lateinit var clientProcessManager: ZMQProcessManager

    private val receiveTimeout = 1000

    @Before
    fun setUp() {
        logger.info("Running test setUp")

        serverLogic = SingleGeoBrokerServerLogic()
        serverLogic.loadConfiguration(Configuration())
        serverLogic.initializeFields()
        serverLogic.startServer()

        loadBalancerLogic = LoadBalancerLogic()
        loadBalancerLogic.loadConfiguration(kg.shabykeev.loadbalancer.commons.server.Configuration.readConfiguration("lb_configuration.toml"))
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
    fun testSubscribeUnsubscribe() {
        // connect, ping, and disconnect
        val cI = "testClient"
        val l = Location.random()
        val g = Geofence.circle(l, 0.4)
        val t = Topic("test")

        val client = SimpleClient("localhost", 7225, clientProcessManager, cI)
        client.send(CONNECTPayload(l))
        client.send(SUBSCRIBEPayload(t, g))

        sleepNoLog(500, 0)

        // validate payloads
        assertTrue(client.receiveWithTimeout(receiveTimeout) is CONNACKPayload)
        assertTrue(client.receiveWithTimeout(receiveTimeout) is SUBACKPayload)
        assertEquals(1, serverLogic.clientDirectory.getCurrentClientSubscriptions(cI).toLong())
        logger.info("Client has successfully subscribed")

        // Unsubscribe
        client.send(UNSUBSCRIBEPayload(t))

        val payload = client.receiveWithTimeout(receiveTimeout)
        if (payload is UNSUBACKPayload) {
            assertEquals(ReasonCode.Success, payload.reasonCode)
        } else {
            fail("Wrong payload, received $payload")
        }
        assertEquals(0, serverLogic.clientDirectory.getCurrentClientSubscriptions(cI).toLong())
    }

    @Test
    fun testUnsubscribeNotConnected() {
        // connect, ping, and disconnect
        val t = Topic("test")
        val cI = "testClient"

        val client = SimpleClient("localhost", 7225, clientProcessManager, cI)

        // Unsubscribe
        client.send(UNSUBSCRIBEPayload(t))

        val payload = client.receiveWithTimeout(receiveTimeout)
        if (payload is UNSUBACKPayload) {
            assertEquals(ReasonCode.NoSubscriptionExisted, payload.reasonCode)
        } else {
            fail("Wrong payload, received $payload")
        }
        assertEquals(0, serverLogic.clientDirectory.getCurrentClientSubscriptions(cI).toLong())
    }

}
