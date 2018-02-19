package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;

/**
 * A test which will randomly add, remove, utilize and stop nodes in a mesh cluster.
 */
public class ChaosClusterTest extends AbstractClusterTest {

	private static Random random = new Random();

	private static final int STARTUP_TIMEOUT = 40;

	private static final int TOTAL_ACTIONS = 30;

	private static final String CLUSTERNAME = "dummy";

	private static final int SERVER_LIMIT = 4;

	private static final List<MeshDockerServer> servers = new ArrayList<>(SERVER_LIMIT);

	private static final List<String> userUuids = new ArrayList<>();

	private static int nAction = 0;

	private enum Actions {
		ADD, REMOVE, UTILIZE, STOP, START;

		public static Actions random() {
			return values()[random.nextInt(values().length)];
		}
	};

	@Test
	public void runTest() throws InterruptedException {
		startInitialServer();

		while (nAction < TOTAL_ACTIONS) {
			printTopology();
			applyAction();
			Thread.sleep(450);
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
		for (MeshDockerServer server : servers) {
			System.err.println(
				"- " + server.getContainerId() + "\t" + server.getNodeName() + "\t" + server.isRunning() + "\t" + server.getContainerIpAddress());
		}
		System.err.println("-----------------------------------");
	}

	private void startInitialServer() throws InterruptedException {
		MeshDockerServer server = new MeshDockerServer(vertx)
			.withInitCluster()
			.withClusterName(CLUSTERNAME)
			.withNodeName("master")
			.withClearFolders()
			.withDataPathPostfix("master")
			.waitForStartup();

		server.start();
		server.awaitStartup(STARTUP_TIMEOUT);
		server.login();
		servers.add(server);
	}

	private void applyAction() throws InterruptedException {
		while (true) {
			switch (Actions.random()) {
			case ADD:
				if (runningServers().size() < TOTAL_ACTIONS) {
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
				if (!runningServers().isEmpty()) {
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
				if (!stoppedServers().isEmpty()) {
					startServer();
					return;
				}
				break;
			}
		}
	}

	private void startServer() throws InterruptedException {
		List<MeshDockerServer> list = stoppedServers();
		MeshDockerServer s = list.get(random.nextInt(list.size()));
		System.err.println("Starting server: " + s.getNodeName());
		String name = s.getNodeName();
		String dataPrefix = s.getDataPathPostfix();
		s.close();
		servers.remove(s);

		MeshDockerServer server = addSlave(CLUSTERNAME, name, dataPrefix, false);
		server.awaitStartup(STARTUP_TIMEOUT);
		server.login();
		servers.add(server);
	}

	private void addServer() throws InterruptedException {
		String name = randomName();
		System.err.println("Adding server: " + name);
		MeshDockerServer server = addSlave(CLUSTERNAME, name, name, false);
		server.awaitStartup(STARTUP_TIMEOUT);
		server.login();
		servers.add(server);
	}

	private void stopServer() {
		MeshDockerServer s = randomServer();
		System.err.println("Stopping server: " + s.getNodeName());
		s.stop();
	}

	private void utilizeServer() {
		List<MeshDockerServer> list = runningServers();
		MeshDockerServer s = list.get(random.nextInt(list.size()));
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
		boolean isAlone = servers.size() <= 1;
		boolean firstHalf = nAction < (TOTAL_ACTIONS / 2);
		boolean reachedLimit = servers.size() >= SERVER_LIMIT;
		return reachedLimit || (!isAlone && !firstHalf);
	}

	private void removeServer() {
		MeshDockerServer s = randomServer();
		System.err.println("Removing server: " + s.getNodeName());
		s.stop();
		servers.remove(s);
	}

	public MeshDockerServer randomServer() {
		int n = random.nextInt(servers.size());
		return servers.get(n);
	}

	public List<MeshDockerServer> runningServers() {
		return servers.stream().filter(MeshDockerServer::isRunning).collect(Collectors.toList());
	}

	public List<MeshDockerServer> stoppedServers() {
		return servers.stream().filter(s -> !s.isRunning()).collect(Collectors.toList());
	}

	private void assertCluster() {
		for (MeshDockerServer server : servers) {
			if (server.isRunning()) {
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
}
