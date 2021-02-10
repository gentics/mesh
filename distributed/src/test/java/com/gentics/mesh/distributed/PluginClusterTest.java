package com.gentics.mesh.distributed;

import static com.gentics.mesh.core.rest.plugin.PluginStatus.PRE_REGISTERED;
import static com.gentics.mesh.core.rest.plugin.PluginStatus.REGISTERED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.rest.client.MeshWebsocket;
import com.gentics.mesh.test.docker.MeshContainer;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * These tests require the test plugins to be build. You can build these plugins using the /core/build-test-plugins.sh script.
 */
@Ignore
public class PluginClusterTest extends AbstractClusterTest {

	private static String clusterPostFix = randomUUID();

	private static final int STARTUP_TIMEOUT = 500;

	private static final Logger log = LoggerFactory.getLogger(PluginClusterTest.class);

	@ClassRule
	public static MeshContainer serverA = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName(clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.waitForStartup()
		.withWriteQuorum(2)
		.withPlugin(new File("../core/target/test-plugins/basic/target/basic-plugin-0.0.1-SNAPSHOT.jar"), "basic.jar")
		.withClearFolders();

	@BeforeClass
	public static void waitForNodes() throws InterruptedException {
		LoggingConfigurator.init();
		serverA.awaitStartup(STARTUP_TIMEOUT);
	}

	@Before
	public void setupLogin() {
		login(serverA);
	}

	@Test
	public void testPluginDeployment() throws InterruptedException {
		// With one node the quorum is not reached and thus the plugin should not be registered.
		assertNoPluginRegistration(serverA, 3000);
		MeshContainer serverB = addSlave("nodeB");
		try {
			waitForPluginRegistration(serverA, 3000);
			waitForPluginRegistration(serverB, 3000);
		} finally {
			serverB.stop();
		}
	}

	/**
	 * Assert that no plugin registration happens within the given time.
	 * 
	 * @param server
	 * @param timeInMilliseconds
	 * @throws InterruptedException
	 */
	private void assertNoPluginRegistration(MeshContainer container, int timeInMilliseconds) throws InterruptedException {
		Thread.sleep(timeInMilliseconds);
		PluginListResponse plugins = call(() -> container.client().findPlugins());
		assertEquals("One plugin should be listed.", 1, plugins.getData().size());
		assertEquals("The plugin should still be registered.", PRE_REGISTERED, plugins.getData().get(0).getStatus());
	}

	private MeshContainer addSlave(String nodeName) throws InterruptedException {
		MeshContainer server = prepareSlave(clusterPostFix, nodeName, randomToken(), true, 2)
			.withPlugin(new File("../core/target/test-plugins/basic/target/basic-plugin-0.0.1-SNAPSHOT.jar"), "basic.jar");
		server.start();
		server.awaitStartup(STARTUP_TIMEOUT);
		login(server);
		return server;
	}

	/**
	 * Check whether the plugin can be found and is registered. Otherwise fail after the given timeout is exceeded.
	 * 
	 * @param container
	 * @param timeoutInMilliseconds
	 * @throws InterruptedException
	 */
	private void waitForPluginRegistration(MeshContainer container, int timeoutInMilliseconds) throws InterruptedException {
		// 1. Initial check
		PluginListResponse plugins = call(() -> container.client().findPlugins());
		if (plugins.getData().size() == 1) {
			PluginResponse plugin = plugins.getData().get(0);
			if (REGISTERED == plugin.getStatus()) {
				log.info("Plugin registered in container {}", container.getNodeName());
				return;
			}
		}
		// Wait for event
		MeshWebsocket eb = container.client().eventbus();
		eb.registerEvents(MeshEvent.PLUGIN_REGISTERED);
		CountDownLatch latch = new CountDownLatch(1);
		eb.events().subscribe(ignore -> {
			latch.countDown();
		}, err -> err.printStackTrace());

		latch.await(timeoutInMilliseconds, TimeUnit.MILLISECONDS);
	}

	private void login(MeshContainer server) {
		server.client().setLogin("admin", "admin");
		server.client().login().blockingGet();
	}

}
