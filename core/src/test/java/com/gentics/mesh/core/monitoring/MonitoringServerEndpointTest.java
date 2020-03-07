package com.gentics.mesh.core.monitoring;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.metric.SimpleMetric;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.orientechnologies.orient.core.OConstants;

import io.vertx.core.impl.launcher.commands.VersionCommand;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class MonitoringServerEndpointTest extends AbstractMeshTest {

	@Before
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
}
