package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.forms.config.ReCaptchaOptions;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Base class for tests posting form data.
 */
public class AbstractFormPostDataTest extends AbstractFormsPluginIntegrationTest {

	/**
	 * web server mock which will act as verification server
	 */
	protected MockWebServer webServer;

	public void setup() {
		super.setup();

		// start the mock recaptcha verification server
		webServer = new MockWebServer();
		try {
			webServer.start();
		} catch (IOException e) {
			fail("Failed to start mock web-server");
		}

		// modify the plugin configuration: set the recaptcha verification URL
		ReCaptchaOptions reCaptchaOptions = FormsTestPlugin.instance.configuration.getReCaptcha();
		reCaptchaOptions.setVerificationUrl(String.format("http://%s:%s/", webServer.getHostName(), webServer.getPort()));

		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
		setLanguages(PROJECT_NAME, Arrays.asList("en", "it"));
	}

	public void tearDown() throws IOException {
		if (webServer != null) {
			webServer.shutdown();
		}
	}

	/**
	 * Post the given body to the form
	 * @param uuid form uuid
	 * @param body post body
	 * @return response
	 * @throws IOException
	 */
	protected Pair<Response, Optional<Cookie>> postToForm(String uuid, RequestBody body) throws IOException {
		Request postRequest = new Request.Builder()
			.url(HttpUrl.get(projectUrl("/forms/" + uuid + "/data"))
				.newBuilder().build())
			.header("Referer", "ref")
			.post(body)
			.build();
		Response response = client().newCall(postRequest).execute();
		Optional<Cookie> cookie = Cookie.parseAll(postRequest.url(), response.headers()).stream().filter(c -> FormDataService.COOKIE_NAME_DATA.equals(c.name()))
			.findFirst();

		return Pair.of(response, cookie);
	}

	/**
	 * Assert that the response is a redirect to "ref"
	 * @param response response
	 */
	protected void assertRedirect(Response response) {
		assertThat(response.isRedirect()).as(String.format("Response redirect %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.header("Location")).as("Location Header").isEqualTo("ref");
	}

	/**
	 * Assert that the response is 205 (Reset Content)
	 * @param response response
	 */
	protected void assertOk(Response response) {
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.code()).as("Response code").isEqualTo(205);
	}

	/**
	 * Assert that there is a single data node for the form
	 * @param formUuid form uuid
	 * @param asserter optional asserter for the data node
	 * @return data node
	 */
	protected NodeResponse assertSingleDataNode(String formUuid, Consumer<NodeResponse> asserter) {
		NodeResponse dataNode = getSingleFormDataNode(formUuid);

		if (asserter != null) {
			asserter.accept(dataNode);
		}

		return dataNode;
	}
}
