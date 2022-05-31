package com.gentics.mesh.core.monitoring;

import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import org.junit.Test;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class HealthEndpointTest extends AbstractHealthTest {

	@Test
	public void testReadinessProbe() {
		call(() -> client().ready());
		meshApi().setStatus(MeshStatus.SHUTTING_DOWN);
		call(() -> client().ready(), SERVICE_UNAVAILABLE, "error_internal");
	}

	@Test
	public void testLivenessProbe() {
		call(() -> client().live());
	}

	@Test
	public void testWritableProbe() {
		call(() -> client().writable());
	}
}
