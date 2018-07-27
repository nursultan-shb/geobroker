package de.hasenburg.geofencebroker.tasks;

import de.hasenburg.geofencebroker.communication.RouterCommunicator;
import de.hasenburg.geofencebroker.main.Utility;
import de.hasenburg.geofencebroker.model.connections.ConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import zmq.socket.reqrep.Router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskManager {

	private static final Logger logger = LogManager.getLogger();

	private final ExecutorService pool = Executors.newCachedThreadPool();

	// if true, the TaskManager stores how often it ran each task
	private boolean storingHistory = false;

	private final AtomicInteger[] runningTasks = new AtomicInteger[TaskName.values().length];
	private final AtomicInteger[] taskHistory = new AtomicInteger[TaskName.values().length];

	public enum TaskName {
		SLEEP, MESSAGE_PROCESSOR_TASK, ZMQ_MESSAGE_PROCESSOR_TASK
	}

	public void tearDown() {
		pool.shutdownNow();
		logger.info("Shutdown Task Manager");
	}
	
	public void storeHistory() {
		storingHistory = true;
	}

	public TaskManager() {
		for (int i = 0; i < runningTasks.length; i++) {
			runningTasks[i] = new AtomicInteger(0);
			taskHistory[i] = new AtomicInteger(0);
		}
		logger.info("Started Task Manager");
	}

	public void registerTask(TaskName name) {
		runningTasks[name.ordinal()].incrementAndGet();
		if (storingHistory) {
			taskHistory[name.ordinal()].incrementAndGet();
		}
	}

	public void deregisterTask(TaskName name) {
		runningTasks[name.ordinal()].decrementAndGet();
	}

	public Map<TaskName, Integer> getRunningTaskNumbers() {
		Map<TaskName, Integer> res = new HashMap<>();
		for (int i = 0; i < runningTasks.length; i++)
			res.put(TaskName.values()[i], runningTasks[i].get());
		return res;
	}

	/**
	 * Returns how often each task was executed. Note, that also tasks that are being executed
	 * are included.
	 * 
	 * @return see above
	 */
	public Map<TaskName, Integer> getHistoricTaskNumbers() {
		Map<TaskName, Integer> res = new HashMap<>();
		for (int i = 0; i < taskHistory.length; i++)
			res.put(TaskName.values()[i], taskHistory[i].get());
		return res;
	}

	public void deleteAllData() {
		for (AtomicInteger ai : runningTasks) {
			ai.set(0);
		}
	}

	/*
	 * ------ Task Initiators ------
	 */

	public Future<Boolean> runSleepTask(int time) {
		return pool.submit(new SleepTask(this, time));
	}

	public Future<Boolean> runMessageProcessorTask(BlockingQueue<ZMsg> messageQueue, RouterCommunicator routerCommunicator, ConnectionManager connectionManager) {
		return pool.submit(new MessageProcessorTask(this, messageQueue, routerCommunicator, connectionManager));
	}

	public Future<Boolean> runZMQMessageProcessorTask(ZContext zContext, ConnectionManager connectionManager) {
		return pool.submit(new ZMQMessageProcessorTask(this, zContext, connectionManager));
	}

}
