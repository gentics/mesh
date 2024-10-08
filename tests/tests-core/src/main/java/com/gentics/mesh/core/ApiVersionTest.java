package com.gentics.mesh.core;

import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import org.junit.Test;

import java.io.IOException;

import static com.gentics.mesh.MeshVersion.CURRENT_API_VERSION;
import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(elasticsearch = NONE, testSize = FULL, startServer = true)
public class ApiVersionTest extends AbstractMeshTest {

	private void expectFailure(String version) throws IOException {
		assertFailure(
			httpGetNowJson(String.format("/api/%s/users", version)),
			"error_version_not_found", version, "v" + CURRENT_API_VERSION
		);
	}

	@Test
	public void testInvalidVersion() throws IOException {
		expectFailure("bogus");
	}

	@Test
	public void testTooLargeVersion() throws IOException {
		expectFailure("v99999999999999999999999999");
	}

	@Test
	public void testFutureVersion() throws IOException {
		expectFailure("v" + (CURRENT_API_VERSION + 1));
	}
}
