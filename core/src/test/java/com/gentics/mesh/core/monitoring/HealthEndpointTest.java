package com.gentics.mesh.core.monitoring;

import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class HealthEndpointTest extends AbstractMeshTest {

	/**
	 * Make sure that the status will be reset after the test so that 
	 * the database setup and initial login will not fail.
	 */
	@After
	@Before
	public void setup() {
		meshApi().setStatus(MeshStatus.READY);
	}

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
