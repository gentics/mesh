package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.io.File;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * These tests require the test plugins to be build. You can build these plugins using the /core/build-test-plugins.sh script. 
 */
public class PluginClusterTest extends AbstractClusterTest {

	private static String clusterPostFix = randomUUID();

	private static final int STARTUP_TIMEOUT = 500;

	private static final Logger log = LoggerFactory.getLogger(PluginClusterTest.class);

	public static MeshDockerServer serverA = new MeshDockerServer(vertx)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.waitForStartup()
		.withWriteQuorum(2)
		.withPlugin(new File("../core/target/test-plugins/basic/target/basic-plugin-0.0.1-SNAPSHOT.jar"), "basic.jar")
		.withClearFolders();

	public static MeshDockerServer serverB = new MeshDockerServer(vertx)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withWriteQuorum(2)
		.withDataPathPostfix(randomToken())
		.withPlugin(new File("../core/target/test-plugins/basic/target/basic-plugin-0.0.1-SNAPSHOT.jar"), "basic.jar")
		.withClearFolders();

	public static MeshRestClient clientA;
	public static MeshRestClient clientB;

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverA).around(serverB);

	@BeforeClass
	public static void waitForNodes() throws InterruptedException {
		LoggingConfigurator.init();
		serverB.awaitStartup(STARTUP_TIMEOUT);
		clientA = serverA.client();
		clientB = serverB.client();
	}

	@Before
	public void setupLogin() {
		clientA.setLogin("admin", "admin");
		clientA.login().blockingGet();
		clientB.setLogin("admin", "admin");
		clientB.login().blockingGet();
	}

	@Test
	public void testPluginDeployment() throws InterruptedException {
		Thread.sleep(6000);
		PluginListResponse pluginsA = call(() -> clientA.findPlugins());
		System.out.println(pluginsA.toJson());
		
		Thread.sleep(6000);
		pluginsA = call(() -> clientA.findPlugins());
		System.out.println(pluginsA.toJson());

		Thread.sleep(6000);
		PluginListResponse pluginsB = call(() -> clientB.findPlugins());
		System.out.println(pluginsB.toJson());

	}

}
