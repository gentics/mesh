package com.gentics.mesh.plugin;

import static com.gentics.mesh.core.rest.plugin.PluginStatus.FAILED_RETRY;
import static com.gentics.mesh.core.rest.plugin.PluginStatus.REGISTERED;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.context.helper.ExpectedEvent;

/**
 * Run the plugin tests in clustered mode with a single instance.
 */
@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = false, clusterMode = true)
public class PluginManagerClusterTest extends PluginManagerTest {
	/**
	 * Test initialization retry, if plugin fails due to topology change
	 * @throws TimeoutException
	 */
	@Test
	public void testRetry() throws TimeoutException {
		options().getClusterOptions().setTopologyLockDelay(1);
		MeshPluginManager manager = pluginManager();

		// we expect the deployment to fail
		try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_DEPLOY_FAILED, 10_000)) {
			// deploy the plugin
			manager.deploy(BackupPlugin.class, "backup").blockingAwait();
		}

		assertEquals(1, manager.getPluginIds().size());
		// plugin status is expected to be FAILED_RETRY, because the plugin invoked the OrientDB backup right before failing.
		// this provoked the "cluster topology change"
		PluginStatus status = manager.getStatus(manager.getPluginIds().iterator().next());
		assertEquals(FAILED_RETRY, status);

		// when the backup is finished, the OrientDB status should go ONLINE again and the PluginManager is
		// expected to retry plugin initialization, which should succeed.
		try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_REGISTERED, 60_000)) {
		}
		status = manager.getStatus(manager.getPluginIds().iterator().next());
		assertEquals(REGISTERED, status);
	}
}
