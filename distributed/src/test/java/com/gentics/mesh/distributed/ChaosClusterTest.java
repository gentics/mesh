package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;

/**
 * A test which will randomly add, remove, utilize and stop nodes in a mesh cluster.
 */
public class ChaosClusterTest extends AbstractClusterTest {

	private static String clusterPostFix = randomUUID();

	private static Random random = new Random();

	private static final int STARTUP_TIMEOUT = 40;

	private static final int TOTAL_ACTIONS = 30;

	private static final String CLUSTERNAME = "dummy";

	private static final int SERVER_LIMIT = 8;

	private static final List<MeshDockerServer> runningServers = new ArrayList<>(SERVER_LIMIT);

	private static final List<MeshDockerServer> stoppedServers = new ArrayList<>(SERVER_LIMIT);

	private static final List<String> userUuids = new ArrayList<>();

	private static int nAction = 0;

	private enum Actions {
		ADD, REMOVE, UTILIZE, STOP, START;

		public static Actions random() {
			return values()[random.nextInt(values().length)];
		}
	};

	@Test
	@Ignore("Fails on CI pipeline. See https://github.com/gentics/mesh/issues/608")
	public void runTest() throws InterruptedException, IOException {
		startInitialServer();

		while (nAction < TOTAL_ACTIONS) {
			printTopology();
			// System.out.println("Press any key to continue");
			// System.in.read();
			System.out.println("\n\n\nApplying action...");
			applyAction();
			Thread.sleep(15_000);
			System.out.println("\n\n\nChecking cluster...");
			assertCluster();
			nAction++;
		}
	}

	private void printTopology() {
		System.err.println("-----------------------------------");
		System.err.println("- Action: " + nAction);
		System.err.println("- Uuids:  " + userUuids.size());
		System.err.println("- Nodes in the cluster:");
		System.err.println("-----------------------------------");
		System.err.println("- ID, Nodename, Running, IP");
		System.err.println("-----------------------------------");
		for (MeshDockerServer server : runningServers) {
			System.err.println(
				"- " + server.getContainerId() + "\t" + server.getNodeName() + "\t" + server.getContainerIpAddress());
		}

		System.err.println("Stopped servers:");
		System.err.println("-----------------------------------");
		for (MeshDockerServer server : stoppedServers) {
			System.err.println(
				"- " + server.getContainerId() + "\t" + server.getNodeName() + "\t" + server.getContainerIpAddress());
		}
		System.err.println("-----------------------------------");
	}

	private void startInitialServer() throws InterruptedException {
		@SuppressWarnings("resource")
		MeshDockerServer server = new MeshDockerServer(vertx)
			.withInitCluster()
			.withClusterName(CLUSTERNAME + clusterPostFix)
			.withNodeName("master")
			.withClearFolders()
			.withDataPathPostfix("master")
			.waitForStartup();

		server.start();
		server.awaitStartup(STARTUP_TIMEOUT);
		server.login();
		runningServers.add(server);
	}

	private void applyAction() throws InterruptedException {
		while (true) {
			switch (Actions.random()) {
			case ADD:
				if (runningServers.size() < SERVER_LIMIT) {
					addServer();
					return;
				}
				break;
			case REMOVE:
				if (allowStopOrRemoval()) {
					removeServer();
					return;
				}
				break;
			case UTILIZE:
				if (!runningServers.isEmpty()) {
					utilizeServer();
					return;
				}
				break;
			case STOP:
				if (allowStopOrRemoval()) {
					stopServer();
					return;
				}
				break;
			case START:
				if (!stoppedServers.isEmpty() && runningServers.size() < SERVER_LIMIT) {
					startServer();
					return;
				}
				break;
			}
		}
	}

	private void startServer() throws InterruptedException {
		MeshDockerServer s = stoppedServers.get(random.nextInt(stoppedServers.size()));
		System.err.println("Starting server: " + s.getNodeName());
		String name = s.getNodeName();
		String dataPrefix = s.getDataPathPostfix();
		stoppedServers.remove(s);

		MeshDockerServer server = addSlave(CLUSTERNAME + clusterPostFix, name, dataPrefix, false);
		server.awaitStartup(STARTUP_TIMEOUT);
		server.login();
		runningServers.add(server);
	}

	private void addServer() throws InterruptedException {
		String name = randomName();
		System.err.println("Adding server: " + name);
		MeshDockerServer server = addSlave(CLUSTERNAME + clusterPostFix, name, name, false);
		server.awaitStartup(STARTUP_TIMEOUT);
		server.login();
		runningServers.add(server);
	}

	private void stopServer() {
		MeshDockerServer s = randomServer();
		System.err.println("Stopping server: " + s.getNodeName());
		s.close();
		runningServers.remove(s);
		stoppedServers.add(s);
	}

	private void utilizeServer() {
		System.err.println("Utilize server...");
		MeshDockerServer s = runningServers.get(random.nextInt(runningServers.size()));
		UserCreateRequest request = new UserCreateRequest();
		request.setPassword("somepass");
		request.setUsername(randomName());
		UserResponse response = call(() -> s.client().createUser(request));
		String uuid = response.getUuid();
		System.err.println("Using server: " + s.getNodeName() + " - Created user {" + uuid + "}");
		userUuids.add(uuid);
	}

	/**
	 * Allow removal and stopping if the server limit is reached or if the server is not alone and not in the first half of the actions.
	 * 
	 * @return
	 */
	private boolean allowStopOrRemoval() {
		boolean isAlone = runningServers.size() <= 1;
		boolean firstHalf = nAction < (TOTAL_ACTIONS / 2);
		boolean reachedLimit = runningServers.size() >= SERVER_LIMIT;
		return reachedLimit || (!isAlone && !firstHalf);
	}

	private void removeServer() {
		MeshDockerServer s = randomServer();
		System.err.println("Removing server: " + s.getNodeName());
		s.stop();
		runningServers.remove(s);
	}

	public MeshDockerServer randomServer() {
		int n = random.nextInt(runningServers.size());
		return runningServers.get(n);
	}

	private void assertCluster() {
		for (MeshDockerServer server : runningServers) {
			// Verify that all created users can be found on the server
			for (String uuid : userUuids) {
				try {
					call(() -> server.client().findUserByUuid(uuid));
				} catch (AssertionError e) {
					e.printStackTrace();
					fail("Error while checking server {" + server.getNodeName() + "} and user {" + uuid + "}");
				}
			}
		}
	}
}
