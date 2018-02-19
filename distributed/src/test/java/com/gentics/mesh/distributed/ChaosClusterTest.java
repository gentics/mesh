package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;

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
		System.err.println("- Nodes in the cluster");
		System.err.println("- Action: " + nAction);
		System.err.println("- Uuids:  " + userUuids.size());
		System.err.println("-----------------------------------");
		for (MeshDockerServer server : servers) {
			System.err.println("- " + server.getNodeName() + "\t" + server.isRunning());
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
		server.awaitStartup(30);
		server.login();
		servers.add(server);
	}

	private void applyAction() throws InterruptedException {
		while (true) {
			switch (Actions.random()) {
			case ADD:
				addServer();
				return;
			case REMOVE:
				if (!allowStopRemove()) {
					continue;
				}
				removeServer();
				return;
			case UTILIZE:
				if (runningServers().isEmpty()) {
					continue;
				}
				utilizeServer();
				return;
			case STOP:
				if (!allowStopRemove()) {
					continue;
				}
				stopServer();
				return;
			case START:
				if (stoppedServers().isEmpty()) {
					continue;
				}
				startServer();
				return;
			}
		}
	}

	private void startServer() throws InterruptedException {
		List<MeshDockerServer> list = stoppedServers();
		MeshDockerServer s = list.get(random.nextInt(list.size()));
		System.err.println("Starting server: " + s.getNodeName());
		String name = s.getNodeName();
		String dataPrefix = s.getDataPathPostfix();
		servers.remove(s);

		MeshDockerServer server = addSlave(CLUSTERNAME, name, dataPrefix, false);
		server.awaitStartup(20);
		server.login();
		servers.add(server);
	}

	private void addServer() throws InterruptedException {
		String name = randomName();
		System.err.println("Adding server: " + name);
		MeshDockerServer server = addSlave(CLUSTERNAME, name, name, false);
		server.awaitStartup(20);
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
		System.err.println("Using server: " + s.getNodeName());
		UserCreateRequest request = new UserCreateRequest();
		request.setPassword("somepass");
		request.setUsername(randomName());
		UserResponse response = call(() -> s.client().createUser(request));
		userUuids.add(response.getUuid());
	}

	/**
	 * Allow removal and stopping if the server limit is reached or if the server is not alone and not in the first half of the actions.
	 * 
	 * @return
	 */
	private boolean allowStopRemove() {
		boolean isAlone = servers.size() <= 1;
		boolean firstHalf = nAction < (TOTAL_ACTIONS / 2);
		boolean reachedLlimit = servers.size() >= SERVER_LIMIT;
		return reachedLlimit || (!isAlone && !firstHalf);
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
					call(() -> server.client().findUserByUuid(uuid));
				}
			}
		}
	}
}
