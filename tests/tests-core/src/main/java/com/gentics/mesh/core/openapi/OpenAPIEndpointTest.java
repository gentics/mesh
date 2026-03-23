package com.gentics.mesh.core.openapi;

import static com.gentics.mesh.test.ClientHelper.call;
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
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

@MeshTestSetting(testSize = FULL, startServer = true)
@RunWith(Parameterized.class)
public class OpenAPIEndpointTest extends AbstractMeshTest {

	@Parameters(name = "{index}: no Mesh Client = {0}, OpenAPI disabled: {1}")
	public static Collection<Object[]> parameters() throws Exception {
		List<Object[]> params = new ArrayList<>();
		for (boolean noClient : new Boolean[] {false, true}) {
			for (boolean apiDisabled : new Boolean[] {false, true}) {
				params.add(new Object[] {noClient, apiDisabled});
			}
		}
		return params;
	}

	@Parameter(0)
	public boolean noClient;

	@Parameter(1)
	public boolean apiDisabled;

	@Before
	public void setup() {
		testContext.getOptions().setServeOpenApi(!apiDisabled);
	}

	@Test
	public void testOpenAPI() throws IOException {
		String input = null;
		if (noClient) {
			input = "http://localhost:" 
					+ options().getHttpServerOptions().getPort() 
					+ MeshVersion.CURRENT_API_BASE_PATH 
					+ "/openapi.yaml";
		} else {
			grantAdmin();
			if (apiDisabled) {
				call(() -> client().getOpenAPI(), HttpResponseStatus.FORBIDDEN);
				return;
			} else {
				input = call(() -> client().getOpenAPI());
			}
		}
		assertNoErrors(input);
	}

	protected OpenAPI assertNoErrors(String input) {
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
			assertThat(result.getOpenAPI()).as("Parsed API").isNotNull();
			assertThat(result.getMessages()).as("Error messages").isNullOrEmpty();
		}
		return result.getOpenAPI();
	}
}
