package de.hasenburg.geobroker.client.main

import de.hasenburg.geobroker.commons.communication.ZMQProcessManager
import de.hasenburg.geobroker.commons.model.message.Payload
import de.hasenburg.geobroker.commons.model.message.ReasonCode
import de.hasenburg.geobroker.commons.model.message.Topic
import de.hasenburg.geobroker.commons.model.spatial.Geofence
import de.hasenburg.geobroker.commons.model.spatial.Location
import de.hasenburg.geobroker.commons.sleepNoLog
import org.apache.logging.log4j.LogManager
import kotlin.system.exitProcess

private val logger = LogManager.getLogger()

fun main() {
    val processManager = ZMQProcessManager()
    val client = SimpleClient("localhost", 7225, processManager)

    // connect
    client.send(Payload.CONNECTPayload(Location.random()))

    // receive one message
    logger.info("Received server answer: {}", client.receive())

    // subscribe
    client.send(Payload.SUBSCRIBEPayload(Topic("red"), Geofence.circle(Location.random(), 2.0)))

    // receive one message
    logger.info("Received server answer: {}", client.receive())

    // wait 5 seconds
    sleepNoLog(5000, 0)

    // disconnect
    client.send(Payload.DISCONNECTPayload(ReasonCode.NormalDisconnection))

    client.tearDownClient()
    if (processManager.tearDown(3000)) {
        logger.info("SimpleClient shut down properly.")
    } else {
        logger.fatal("ProcessManager reported that processes are still running: {}",
                processManager.incompleteZMQProcesses)
    }
    exitProcess(0)
}