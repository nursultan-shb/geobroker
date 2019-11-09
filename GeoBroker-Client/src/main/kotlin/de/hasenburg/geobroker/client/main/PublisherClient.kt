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
    val location = Location.random();
    client.send(Payload.CONNECTPayload(location))

    // receive one message
    logger.info("Received server answer: {}", client.receive())

    // publish

    logger.info("-----------Sending RED")
    for (i in 1..100) {
        client.send(Payload.PUBLISHPayload(Topic("red"), Geofence.circle(location, 2.0), i.toString()))
        logger.info("Received server answer: {}", client.receive())
    }

    // publish

    logger.info("-----------Sending BLUE")
    for (i in 1..50) {
        client.send(Payload.PUBLISHPayload(Topic("blue"), Geofence.circle(location, 2.0), i.toString()))
        logger.info("Received server answer: {}", client.receive())
    }

    // wait 5 seconds
    sleepNoLog(300*1000, 0)

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