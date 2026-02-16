package com.gentics.mesh.core.openapi;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.core.rest.openapi.Format;
import com.gentics.mesh.core.rest.openapi.Version;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.swagger.parser.OpenAPIParser;
import io.swagger.parser.models.ParseOptions;
import io.swagger.parser.models.SwaggerParseResult;

@MeshTestSetting(testSize = FULL, startServer = true)
@RunWith(Parameterized.class)
public class OpenAPIEndpointTest extends AbstractMeshTest {

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

	@Parameter(0)
	public boolean noClient;

	@Parameter(1)
	public Format format;

	@Parameter(2)
	public Version version;

	@Test
	public void testOpenAPI() {
		String input = null;
		if (noClient) {
			input = "http://localhost:" 
					+ options().getHttpServerOptions().getPort() 
					+ MeshVersion.CURRENT_API_BASE_PATH 
					+ "/openapi?format=" 
					+ format.name().toLowerCase() 
					+ "&version=" + version.name().toLowerCase();
		} else {
			grantAdmin();
			input = call(() -> client().getOpenAPI(format, version));
		}
		assertNoErrors(input);
	}

	protected void assertNoErrors(String input) {
		OpenAPIParser parser = new OpenAPIParser();
		ParseOptions options = new ParseOptions();
		options.setResolve(true);
		options.setResolveFully(true);
		SwaggerParseResult result = noClient 
				? parser.readLocation(input, null, null)
				: parser.readContents(input, null, null);

		assertThat(result.getOpenAPI()).as("Parsed API").isNotNull();
		assertThat(result.getMessages()).as("Error messages").isNullOrEmpty();
	}
}
