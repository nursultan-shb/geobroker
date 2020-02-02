package de.hasenburg.geobroker.server.matching

import de.hasenburg.geobroker.commons.model.KryoSerializer
import de.hasenburg.geobroker.commons.model.message.Payload.*
import de.hasenburg.geobroker.commons.model.message.ReasonCode
import de.hasenburg.geobroker.commons.model.message.loadbalancer.ClientReasonCode
import de.hasenburg.geobroker.commons.model.message.payloadToZMsg
import de.hasenburg.geobroker.server.loadAnalysis.ResourceMetrics
import de.hasenburg.geobroker.server.storage.TopicAndGeofenceMapper
import de.hasenburg.geobroker.server.storage.client.ClientDirectory
import org.apache.logging.log4j.LogManager
import org.zeromq.ZMQ.Socket
import org.zeromq.ZMsg

private val logger = LogManager.getLogger()

/**
 * One GeoBroker instance that does not communicate with others. Uses the [de.hasenburg.geobroker.server.storage.TopicAndGeofenceMapper].
 */
class SingleGeoBrokerMatchingLogic(private val clientDirectory: ClientDirectory,
                                   private val topicAndGeofenceMapper: TopicAndGeofenceMapper) : IMatchingLogic {

    private fun sendResponse(response: ZMsg, clients: Socket) {
        logger.trace("Sending response $response")
        response.send(clients)
    }

    override fun processCONNECT(clientIdentifier: String, payload: CONNECTPayload, clients: Socket,
                                brokers: Socket, kryo: KryoSerializer) {
        val payloadResponse =
                connectClientAtLocalBroker(clientIdentifier, payload.location, clientDirectory, logger)

        val response = payloadToZMsg(payloadResponse, kryo, clientIdentifier)

        sendResponse(response, clients)
    }

    override fun processDISCONNECT(clientIdentifier: String, payload: DISCONNECTPayload, clients: Socket,
                                   brokers: Socket,
                                   kryo: KryoSerializer) {
        val success = clientDirectory.removeClient(clientIdentifier)
        if (!success) {
            logger.trace("Client for {} did not exist", clientIdentifier)
            return
        }

        logger.debug("Disconnected client {}, code {}", clientIdentifier, payload.reasonCode)
        // no response to send here
    }

    override fun processPINGREQ(clientIdentifier: String, payload: PINGREQPayload, clients: Socket,
                                brokers: Socket,
                                kryo: KryoSerializer) {
        val reasonCode = updateClientLocationAtLocalBroker(clientIdentifier,
                payload.location,
                clientDirectory,
                logger)

        val response = payloadToZMsg(PINGRESPPayload(reasonCode), kryo, clientIdentifier)

        sendResponse(response, clients)
    }

    override fun processSUBSCRIBE(clientIdentifier: String, payload: SUBSCRIBEPayload, clients: Socket,
                                  brokers: Socket,
                                  kryo: KryoSerializer) {
        val reasonCode = subscribeAtLocalBroker(clientIdentifier,
                clientDirectory,
                topicAndGeofenceMapper,
                payload.topic,
                payload.geofence,
                logger)

        val response = payloadToZMsg(SUBACKPayload(reasonCode), kryo, clientIdentifier)

        sendResponse(response, clients)
    }

    override fun processUNSUBSCRIBE(clientIdentifier: String, payload: UNSUBSCRIBEPayload, clients: Socket,
                                    brokers: Socket, kryo: KryoSerializer) {
        val reasonCode = unsubscribeAtLocalBroker(clientIdentifier,
                clientDirectory,
                topicAndGeofenceMapper,
                payload.topic,
                logger)

        val response = payloadToZMsg(UNSUBACKPayload(reasonCode), kryo, clientIdentifier)

        sendResponse(response, clients)
    }

    override fun processPUBLISH(clientIdentifier: String, payload: PUBLISHPayload, clients: Socket,
                                brokers: Socket, kryo: KryoSerializer) {

        val reasonCode: ReasonCode
        val publisherLocation = clientDirectory.getClientLocation(clientIdentifier)

        reasonCode = if (publisherLocation == null) { // null if client is not connected
            logger.debug("Client {} is not connected", clientIdentifier)
            ReasonCode.NotConnected
        } else {
            publishMessageToLocalClients(publisherLocation,
                    payload,
                    clientDirectory,
                    topicAndGeofenceMapper,
                    clients,
                    logger,
                    kryo)
        }

        ResourceMetrics.setPublishedMessages(payload.topic, 1);

        // send response to publisher
        val response = payloadToZMsg(PUBACKPayload(reasonCode, payload.content), kryo, clientIdentifier)
        sendResponse(response, clients)
    }

    /*****************************************************************
     * Broker Forward Methods
     ****************************************************************/

    override fun processBrokerForwardDisconnect(otherBrokerId: String,
                                                payload: BrokerForwardDisconnectPayload, clients: Socket,
                                                brokers: Socket, kryo: KryoSerializer) {
        logger.warn("Unsupported operation, message is discarded")
    }

    override fun processBrokerForwardPingreq(otherBrokerId: String, payload: BrokerForwardPingreqPayload,
                                             clients: Socket, brokers: Socket, kryo: KryoSerializer) {
        logger.warn("Unsupported operation, message is discarded")
    }

    override fun processBrokerForwardSubscribe(otherBrokerId: String, payload: BrokerForwardSubscribePayload,
                                               clients: Socket, brokers: Socket, kryo: KryoSerializer) {
        logger.warn("Unsupported operation, message is discarded")
    }

    override fun processBrokerForwardUnsubscribe(otherBrokerId: String,
                                                 payload: BrokerForwardUnsubscribePayload, clients: Socket,
                                                 brokers: Socket, kryo: KryoSerializer) {
        logger.warn("Unsupported operation, message is discarded")
    }

    override fun processBrokerForwardPublish(otherBrokerId: String, payload: BrokerForwardPublishPayload,
                                             clients: Socket, brokers: Socket, kryo: KryoSerializer) {
        logger.warn("Unsupported operation, message is discarded")
    }

    /*****************************************************************
     * LoadBalancer Methods
     ****************************************************************/

    override fun processReqTopicSubscriptions(planCreatorId: String, payload: ReqSubscriptionsPayload,
                                              clients: Socket, brokers: Socket, kryo: KryoSerializer) {
        val subscriptions = clientDirectory.getTopicSubscriptions(payload.topic)
        val response = payloadToZMsg(ReqSubscriptionsAckPayload(payload.taskId, ReasonCode.Success, subscriptions), kryo, planCreatorId)
        sendResponse(response, clients)

        logger.debug("MIGRATION: Requested topic subscriptions: topic {}, size of subscriptions {}", payload.topic, subscriptions.size)
    }

    override fun processInjectSubscriptions(planCreatorId: String, payload: InjectSubscriptionsPayload,
                                            clients: Socket, brokers: Socket, kryo: KryoSerializer) {

        val clientReasonCodes = mutableListOf<ClientReasonCode>()
        for (s in payload.subscriptions) {
            val reasonCode = subscribeAtLocalBroker(s.getClientId(),
                    clientDirectory,
                    topicAndGeofenceMapper,
                    s.topic,
                    s.geofence,
                    logger)

            clientReasonCodes.add(ClientReasonCode(s.getClientId(), reasonCode))
        }

        val response = payloadToZMsg(InjectSubscriptionsAckPayload(payload.taskId, clientReasonCodes), kryo, planCreatorId)
        sendResponse(response, clients)

        logger.debug("MIGRATION: Received injection of subscriptions. Size {}", payload.subscriptions.size)
    }

    override fun processUnsubscribeTopic(planCreatorId: String, payload: UnsubscribeTopicPayload,
                                              clients: Socket, brokers: Socket, kryo: KryoSerializer) {

        val clientReasonCodes = mutableListOf<ClientReasonCode>()
        val subscriptions = clientDirectory.getTopicSubscriptions(payload.topic)
        for (subscription in subscriptions) {
            val reasonCode = unsubscribeAtLocalBroker(subscription.getClientId(),
                    clientDirectory,
                    topicAndGeofenceMapper,
                    subscription.topic,
                    logger)
            clientReasonCodes.add(ClientReasonCode(subscription.getClientId(), reasonCode))
        }

        val response = payloadToZMsg(UnsubscribeTopicAckPayload(payload.taskId, clientReasonCodes), kryo, planCreatorId)
        sendResponse(response, clients)

        logger.debug("MIGRATION: Requested topic unsubscription: topic {}", payload.topic)
    }
}
