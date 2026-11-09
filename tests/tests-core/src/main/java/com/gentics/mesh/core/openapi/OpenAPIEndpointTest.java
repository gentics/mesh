package com.gentics.mesh.core.openapi;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.etc.config.Format;
import com.gentics.mesh.etc.config.Version;
import com.gentics.mesh.parameter.OpenAPIParameters;
import com.gentics.mesh.parameter.impl.OpenAPIParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import okhttp3.Request;
import okhttp3.Response;

@MeshTestSetting(testSize = FULL, startServer = true)
@RunWith(Parameterized.class)
public class OpenAPIEndpointTest extends AbstractMeshTest {

	@Parameters(name = "{index}: no Mesh Client = {0}, OpenAPI disabled = {1}, format = {2}, version = {3}")
	public static Collection<Object[]> parameters() throws Exception {
		List<Object[]> params = new ArrayList<>();
		for (boolean noClient : new Boolean[] {false, true}) {
			for (boolean apiDisabled : new Boolean[] {false, true}) {
				for (Format format : Format.values()) {
					for (Version version : Version.values()) {
						params.add(new Object[] {noClient, apiDisabled, format, version});
					}
				}
			}
		}
		return params;
	}

	@Parameter(2)
	public Format format;

	@Parameter(3)
	public Version version;

	@Test
	public void testOpenAPI() throws IOException {
		String input = null;
		if (noClient) {
			input = String.format("http://localhost:%s%s/openapi?format=%s&version=%s", port(), MeshVersion.CURRENT_API_BASE_PATH, format.name().toLowerCase(), version.name().toLowerCase());
		} else {
			grantAdmin();
			OpenAPIParameters parameters = new OpenAPIParametersImpl().setFormat(format).setVersion(version);
			Response response = httpClient().newCall(new Request.Builder()
					.url(String.format("http://localhost:%s%s/openapi?%s", port(), MeshVersion.CURRENT_API_BASE_PATH, parameters.getQueryParameters()))
					.get()
					.build()
				).execute();
			if (apiDisabled) {
				assertThat(response.code()).as("Response code").isEqualTo(HttpResponseStatus.FORBIDDEN.code());
			} else {
				input = response.body().string();
			}
		}
		assertNoErrors(input);
	}

	protected OpenAPI assertNoErrors(String input) {
		System.out.println(input);
		OpenAPIV3Parser parser = new OpenAPIV3Parser();
		ParseOptions options = new ParseOptions();
		options.setResolve(true);
		options.setResolveFully(true);

		SwaggerParseResult result = noClient 
				? parser.readLocation(input, null, options)
				: parser.readContents(input, null, options);

		if (apiDisabled) {
			assertThat(result.getOpenAPI()).as("Parsed API").isNull();
			assertThat(result.getMessages()).as("Error messages").isNotEmpty();
		} else {
			// https://github.com/swagger-api/swagger-parser/issues/2271
			if (version == Version.V30) {
				assertThat(result.getMessages()).as("Error messages").isNullOrEmpty();
			}
			assertThat(result.getOpenAPI()).as("Parsed API").isNotNull();
			assertThat(result.getOpenAPI().getOpenapi()).as("Spec version").isEqualTo((version == Version.V31) ? "3.1.0" : "3.0.1");
		}
		return result.getOpenAPI();
	}

	@Parameter(0)
	public boolean noClient;

	@Parameter(1)
	public boolean apiDisabled;

	@Before
	public void setup() {
		testContext.getOptions().setServeOpenApi(!apiDisabled);
	}
}
