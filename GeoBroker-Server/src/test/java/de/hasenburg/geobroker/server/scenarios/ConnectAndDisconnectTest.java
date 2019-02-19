package de.hasenburg.geobroker.server.scenarios;

import de.hasenburg.geobroker.commons.model.message.ControlPacketType;
import de.hasenburg.geobroker.commons.model.message.ReasonCode;
import de.hasenburg.geobroker.commons.communication.ZMQProcessManager;
import de.hasenburg.geobroker.server.communication.ZMQProcessStarter;
import de.hasenburg.geobroker.server.main.Configuration;
import de.hasenburg.geobroker.client.main.SimpleClient;
import de.hasenburg.geobroker.commons.Utility;
import de.hasenburg.geobroker.client.communication.InternalClientMessage;
import de.hasenburg.geobroker.server.storage.client.ClientDirectory;
import de.hasenburg.geobroker.commons.exceptions.CommunicatorException;
import de.hasenburg.geobroker.commons.model.message.payloads.CONNECTPayload;
import de.hasenburg.geobroker.commons.model.message.payloads.DISCONNECTPayload;
import de.hasenburg.geobroker.commons.model.spatial.Location;
import de.hasenburg.geobroker.server.storage.TopicAndGeofenceMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConnectAndDisconnectTest {

	private static final Logger logger = LogManager.getLogger();

	ClientDirectory clientDirectory;
	TopicAndGeofenceMapper topicAndGeofenceMapper;
	ZMQProcessManager processManager;

	@SuppressWarnings("Duplicates")
	@Before
	public void setUp() {
		logger.info("Running test setUp");

		clientDirectory = new ClientDirectory();
		topicAndGeofenceMapper = new TopicAndGeofenceMapper(new Configuration());

		processManager = new ZMQProcessManager();
		ZMQProcessStarter.runZMQProcess_Server(processManager, "tcp://localhost", 5559, "broker");
		ZMQProcessStarter.runZMQProcess_MessageProcessor(processManager,"message_processor", clientDirectory, topicAndGeofenceMapper);

		assertEquals(0, clientDirectory.getNumberOfClients());
	}

	@After
	public void tearDown() {
		logger.info("Running test tearDown.");
		assertTrue(processManager.tearDown(5000));
	}

	@Test
	public void testOneClient() throws InterruptedException, CommunicatorException {
		SimpleClient client = new SimpleClient(null, "tcp://localhost", 5559, processManager);

		// connect
		client.sendInternalClientMessage(new InternalClientMessage(ControlPacketType.CONNECT,
																   new CONNECTPayload(Location.random())));
		assertEquals(ControlPacketType.CONNACK, client.receiveInternalClientMessage().getControlPacketType());

		// check whether client exists
		assertEquals(1, clientDirectory.getNumberOfClients());

		// disconnect
		client.sendInternalClientMessage(new InternalClientMessage(ControlPacketType.DISCONNECT,
																   new DISCONNECTPayload(ReasonCode.NormalDisconnection)));

		// check whether disconnected and no more messages received
		Utility.sleepNoLog(1, 0);
		assertEquals(0, clientDirectory.getNumberOfClients());
	}

	@Test
	public void testMultipleClients() throws InterruptedException, CommunicatorException {
		List<SimpleClient> clients = new ArrayList<>();
		int activeConnections = 10;
		Random random = new Random();

		// create clients
		for (int i = 0; i < activeConnections; i++) {
			SimpleClient client = new SimpleClient(null, "tcp://localhost", 5559, processManager);
			clients.add(client);
		}

		// send connects and randomly also disconnect
		for (SimpleClient client : clients) {
			client.sendInternalClientMessage(new InternalClientMessage(ControlPacketType.CONNECT,
																	   new CONNECTPayload(Location.random())));
			if (random.nextBoolean()) {
				client.sendInternalClientMessage(new InternalClientMessage(ControlPacketType.DISCONNECT,
																		   new DISCONNECTPayload(ReasonCode.NormalDisconnection)));
				activeConnections--;
			}
		}

		// check acknowledgements
		for (SimpleClient client : clients) {
			assertEquals(ControlPacketType.CONNACK, client.receiveInternalClientMessage().getControlPacketType());
		}

		Utility.sleepNoLog(1, 0);
		// check number of active clients
		assertEquals("Wrong number of active clients", activeConnections, clientDirectory.getNumberOfClients());
		logger.info("{} out of {} clients were active, so everything fine", activeConnections, 10);
	}

}
