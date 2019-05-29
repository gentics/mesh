package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class DevNullSearchProviderQueryTest extends AbstractMeshTest {

	@Test
	public void testSearchQuery() throws IOException {
		String json = getESText("userWildcard.es");
		call(() -> client().searchUsers(json), SERVICE_UNAVAILABLE, "search_error_no_elasticsearch_configured");
	}
}
