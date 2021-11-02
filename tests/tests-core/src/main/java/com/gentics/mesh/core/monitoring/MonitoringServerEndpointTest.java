package com.gentics.mesh.core.monitoring;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.localconfig.LocalConfigModel;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.metric.SimpleMetric;
import com.gentics.mesh.plugin.FailingInitializePlugin;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.orientechnologies.orient.core.OConstants;
import io.vertx.core.impl.launcher.commands.VersionCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class MonitoringServerEndpointTest extends AbstractMeshTest {

	/**
	 * Make sure that the status will be reset after the test so that the database setup and initial login will not fail.
	 */
	@Before
	@After
	public void setup() {
		meshApi().setStatus(MeshStatus.READY);
	}

	@Test
	public void testMetrics() {
		for (int i = 0; i < 10; i++) {
			call(() -> client().me());
		}
		String metrics = call(() -> monClient().metrics());
		assertThat(metrics).as("Metrics result").isNotEmpty().contains(SimpleMetric.TX.key());
	}

	@Test
	public void testStatus() {
		meshApi().setStatus(MeshStatus.WAITING_FOR_CLUSTER);
		MeshStatusResponse status = call(() -> monClient().status());
		assertEquals(MeshStatus.WAITING_FOR_CLUSTER, status.getStatus());
	}

	@Test
	public void testReadinessFailingPlugin() {
		MeshPluginManager manager = pluginManager();
		manager.deploy(FailingInitializePlugin.class, "failing").blockingAwait();
		try {
			call(() -> monClient().ready());
			fail("The ready probe should fail");
		} catch (Exception e) {
			// NOOP
		}
		manager.undeploy("failing").blockingAwait(2, TimeUnit.SECONDS);
		assertEquals(0, manager.getPluginIds().size());
		call(() -> monClient().ready());
	}

	@Test
	public void testLivenessFailingPlugin() {
		MeshPluginManager manager = pluginManager();
		manager.deploy(FailingInitializePlugin.class, "failing").blockingAwait();
		try {
			call(() -> monClient().live());
			fail("The liveness probe should fail");
		} catch (Exception e) {
			// NOOP
		}
		manager.undeploy("failing").blockingAwait(2, TimeUnit.SECONDS);
		assertEquals(0, manager.getPluginIds().size());
		call(() -> monClient().live());
	}

	@Test
	public void testClusterStatus() {
		call(() -> monClient().clusterStatus(), BAD_REQUEST, "error_cluster_status_only_available_in_cluster_mode");
	}

	@Test
	public void testReadinessProbe() {
		call(() -> monClient().ready());
		meshApi().setStatus(MeshStatus.SHUTTING_DOWN);
		call(() -> monClient().ready(), SERVICE_UNAVAILABLE, "error_internal");
	}

	@Test
	public void testLivenessProbe() {
		call(() -> monClient().live());
	}

	@Test
	public void testAPIInfo() {
		MeshServerInfoModel info = call(() -> monClient().versions());
		assertEquals(Mesh.getPlainVersion(), info.getMeshVersion());
		assertEquals("orientdb", info.getDatabaseVendor());
		assertEquals("dev-null", info.getSearchVendor());
		assertEquals(VersionCommand.getVersion(), info.getVertxVersion());
		assertEquals(options().getNodeName(), info.getMeshNodeName());
		assertEquals("The database version did not match.", OConstants.getVersion(), info.getDatabaseVersion());
		assertEquals("1.0", info.getSearchVersion());
		assertEquals(db().getDatabaseRevision(), info.getDatabaseRevision());
	}

	@Test
	public void testWritableReturns200() {
		call(() -> monClient().writable());
	}

	@Test
	public void testWritableReturns503WhenReadOnlyMode() {
		call(() -> client().updateLocalConfig(buildLocalConfigModel(true)));
		call(() -> monClient().writable(), SERVICE_UNAVAILABLE, "error_internal");
	}

	private LocalConfigModel buildLocalConfigModel(boolean readOnly) {
		LocalConfigModel localConfigModel = new LocalConfigModel();
		localConfigModel.setReadOnly(readOnly);
		return localConfigModel;
	}
}
