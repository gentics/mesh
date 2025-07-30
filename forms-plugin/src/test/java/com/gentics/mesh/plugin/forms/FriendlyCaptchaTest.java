package com.gentics.mesh.plugin.forms;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.config.FriendlyCaptchaOptions;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.model.FriendlyCaptchaElement;
import com.gentics.mesh.plugin.forms.model.ValidationError;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test Friendly Captcha validation.
 */
public class FriendlyCaptchaTest {
	private Vertx vertx;
	private HttpServer mockServer;
	private int port;

	@Before
	public void setUp() {
		vertx = Vertx.vertx();
		mockServer = vertx.getDelegate().createHttpServer();

		mockServer.requestHandler(request -> request.bodyHandler(body -> {
			String solutionKey = "solution=";
			String solution = Arrays.stream(body.toString().split("&"))
				.filter(param -> param.startsWith(solutionKey))
				.map(param -> param.substring(solutionKey.length()))
				.findFirst()
				.orElse("");
			JsonObject response = new JsonObject();

			if (solution.equals("valid_solution")) {
				response.put("success", true);
			} else if (solution.equals("expired_solution")) {
				response.put("success", false)
					.put("details", "solution expired");
			} else {
				response.put("success", false)
					.put("details", "invalid solution");
			}

			request.response()
				.putHeader("content-type", "application/json")
				.end(response.encode());
		}));

		mockServer.listen(0, "localhost", result -> {
			if (result.succeeded()) {
				port = result.result().actualPort();
			}
		});

		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			throw new RuntimeException("Mock server did not start in time", e);
		}
	}

	@After
	public void tearDown() {
		mockServer.close();
		vertx.close();
	}

	/** Check validation for a valid solution. */
	@Test
	public void testValidSolution() {
		var error = validateCaptcha("valid_solution");

		assertThat(error).isNull();
	}

	/** Check validation for an expired solution. */
	@Test
	public void testExpiredSolution() {
		var error = validateCaptcha("expired_solution");

		assertThat(error).isNotNull();
		assertThat(error.getMessage()).isEqualTo(I18nHelper.get(Locale.GERMAN, FriendlyCaptchaElement.MESSAGE_KEY) + " solution expired");
	}

	/** Check validation for an invalid solution. */
	@Test
	public void testInvalidSolution() {
		var error = validateCaptcha("invalid_solution");

		assertThat(error).isNotNull();
		assertThat(error.getMessage()).isEqualTo(I18nHelper.get(Locale.GERMAN, FriendlyCaptchaElement.MESSAGE_KEY) + " invalid solution");
	}

	/**
	 * Create the options for the {@link FriendlyCaptchaElement} using the mock server for verification URL.
	 * @return Options for the FriendlyCaptchaElement
	 */
	private FriendlyCaptchaOptions createOptions() {
		var options = new FriendlyCaptchaOptions();

		options.setAcceptOnErrorStatus(false);
		options.setSecretKey("fake_secret_key");
		options.setSiteKey("fake_site_key");
		options.setVerificationUrl("http://localhost:" + port);

		return options;
	}

	/**
	 * Validate a {@link FriendlyCaptchaElement} with the given solution.
	 *
	 * @param solution The solution to use for validation.
	 * @return The result of the element validation.
	 */
	private ValidationError validateCaptcha(String solution) {
		var config = new FormsConfig();
		var options = createOptions();

		config.setFriendlyCaptcha(options);

		var element = new FriendlyCaptchaElement();

		element.setConfiguration(config);

		var map = new HeadersMultiMap();

		map.add(options.getSolutionKey(), solution);

		return element.validate(map, Collections.emptySet(), new NodeResponse(), Vertx.vertx(), new OkHttpClient.Builder().build()).blockingGet();
	}
}