package com.gentics.mesh.enterprise.core;

import static com.gentics.mesh.test.TestSize.FULL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.core.openapi.OpenAPIEndpointTest;
import com.gentics.mesh.enterprise.parameter.OpenAPIParameters;
import com.gentics.mesh.enterprise.parameter.client.OpenAPIParametersImpl;
import com.gentics.mesh.etc.config.Format;
import com.gentics.mesh.etc.config.Version;
import com.gentics.mesh.test.MeshTestSetting;

import okhttp3.Request;

@MeshTestSetting(testSize = FULL, startServer = true)
@RunWith(Parameterized.class)
public class EnterpriseOpenAPIEndpointTest extends OpenAPIEndpointTest {

	@Parameters(name = "{index}: no Mesh Client = {0}, format = {1}, version = {2}")
	public static Collection<Object[]> parameters() throws Exception {
		List<Object[]> params = new ArrayList<>();
		for (boolean noClient : new Boolean[] {false, true}) {
			for (Format format : Format.values()) {
				for (Version version : Version.values()) {
					params.add(new Object[] {noClient, format, version});
				}
			}
		}
		return params;
	}

	@Parameter(1)
	public Format format;

	@Parameter(2)
	public Version version;

	@Test
	public void testOpenAPI() throws IOException {
		String input = null;
		if (noClient) {
			input = String.format("http://localhost:%s%s/openapi?format=%s&version=%s", port(), MeshVersion.CURRENT_API_BASE_PATH, format.name().toLowerCase(), version.name().toLowerCase());
		} else {
			grantAdmin();
			OpenAPIParameters parameters = new OpenAPIParametersImpl().setFormat(format).setVersion(version);
			input = httpClient().newCall(new Request.Builder()
					.url(String.format("http://localhost:%s%s/openapi?%s", port(), MeshVersion.CURRENT_API_BASE_PATH, parameters.getQueryParameters()))
					.get()
					.build()
				).execute().body().string();
		}
		assertNoErrors(input);
	}
}

