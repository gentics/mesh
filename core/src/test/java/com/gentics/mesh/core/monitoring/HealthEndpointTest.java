package com.gentics.mesh.core.monitoring;

import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.reactivex.Observable;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class HealthEndpointTest extends AbstractMeshTest {

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
	public void name() {
	}

	@Test
	public void testLivenessProbe() {
		Observable.range(1, 10)
			.flatMap(nr -> cli)
		call(() -> client().live());
	}
}
