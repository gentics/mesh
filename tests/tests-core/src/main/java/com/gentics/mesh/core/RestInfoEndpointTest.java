package com.gentics.mesh.core;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.RxUtil;

import okhttp3.Request;
import okhttp3.Response;

@MeshTestSetting(testSize = FULL, startServer = true)
public class RestInfoEndpointTest extends AbstractMeshTest {

	@Test
	public void testGetInfoWithDisabledServerTokens() {
		options().getHttpServerOptions().setServerTokens(false);
		MeshServerInfoModel info = call(() -> client().getApiInfo());
		assertNull(info.getMeshVersion());

		grantAdmin();
		info = call(() -> client().getApiInfo());
		assertNotNull(info.getMeshVersion());
	}

	@Test
	public void testGetInfo() {
		MeshServerInfoModel info = call(() -> client().getApiInfo());
		assertEquals(Mesh.getPlainVersion(), info.getMeshVersion());
		assertEquals("dev-null", info.getSearchVendor());
		assertEquals(RxUtil.getVertxVersion(), info.getVertxVersion());
		assertEquals(options().getNodeName(), info.getMeshNodeName());
		assertEquals("1.0", info.getSearchVersion());
		assertEquals(db().getDatabaseRevision(), info.getDatabaseRevision());
	}

	@Test
	public void testRAMLEndpointRemoved() throws IOException {
		grantAdmin();
		try (Response response = httpClient().newCall(new Request.Builder()
				.url(String.format("http://localhost:%s%s/raml", port(), MeshVersion.CURRENT_API_BASE_PATH))
				.header("Authorization", "Bearer " + client().getAuthentication().getToken())
				.get()
				.build()).execute()) {
			assertEquals(NOT_FOUND.code(), response.code());
		}
	}
}
