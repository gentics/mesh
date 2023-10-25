package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.forms.config.InjectionPreventionMode;
import com.gentics.mesh.plugin.forms.config.SecurityOptions;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for rejecting or sanitizing potentially unsafe posted values.
 */
@RunWith(Parameterized.class)
public class SecurityFormPostDataTest extends AbstractFormPostDataTest {

	private enum TestSanitationRules {
		EMPTY(),
		REPLACE_DOTS("\\.(\\S)", ". $1");

		private final Map<String, String> rules = new HashMap<>();

		TestSanitationRules(String ...rulesArray) {
			if (rulesArray == null) {
				return;
			}

			if (rulesArray.length % 2 != 0) {
				throw new RuntimeException("Rules must have an even number of elements (pattern -> replacement)");
			}

			for (int i = 0; i < rulesArray.length; i += 2) {
				if (StringUtils.isBlank(rulesArray[i])) {
					throw new RuntimeException("Rule patterns must not be null or empty");
				}

				String replacement = rulesArray[i + 1] == null ? "" : rulesArray[i + 1];

				rules.put(rulesArray[i], replacement);
			}
		}

		public boolean empty() {
			return rules.isEmpty();
		}

		public Map<String, String> rules() {
			return rules;
		}
	}

	@Parameters(name = "{index}: mode {0}, rule definition: {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();

		for (InjectionPreventionMode mode : InjectionPreventionMode.values()) {
			for (TestSanitationRules rules : TestSanitationRules.values()) {
				if (mode == InjectionPreventionMode.NONE && !rules.empty()) {
					// Non-empty sanitation rules make no sense when prevention mode is NONE.
					continue;
				}

				data.add(new Object[] { mode, rules });
			}
		}

		return data;
	}

	@Parameter
	public InjectionPreventionMode mode;

	@Parameter(1)
	public TestSanitationRules ruleDefinition;

	@Before
	public void setup() {
		super.setup();

		SecurityOptions securityOptions = FormsTestPlugin.instance.configuration.getSecurityOptions();

		securityOptions.setInjectionPreventionMode(mode);
		securityOptions.setSanitationRules(ruleDefinition.rules());
	}

	@After
	public void tearDown() throws IOException {
		super.tearDown();
	}

	/**
	 * Verify that valid input is not rejected and not modified by sanitation.
	 */
	@Test
	public void testValidInput() throws IOException {
		String input = "Normal text";

		performTest(input, true, input);
	}

	/**
	 * Verify that input containing markup is either rejected or sanitized.
	 */
	@Test
	public void testMarkup() throws IOException {
		String input = "<a href=evilsite>Normal</a> text<script>alert('Boo!')</script>";
		boolean expectValid = mode != InjectionPreventionMode.REJECT_MARKUP;
		String expectedStoredValue = mode == InjectionPreventionMode.SANITIZE_MARKUP
			? "Normal text"
			: input;

		performTest(input, expectValid, expectedStoredValue);
	}

	/**
	 * Verify that the dot replacement rule either rejects or sanitizes an input
	 * with a URL.
	 */
	@Test
	public void testAbsoluteUrl() throws IOException {
		String input = "Bad link: https://evil.site";
		boolean expectValid = (mode != InjectionPreventionMode.REJECT && mode != InjectionPreventionMode.REJECT_MARKUP)
			|| ruleDefinition != TestSanitationRules.REPLACE_DOTS;
		String expectedStoredValue = (mode == InjectionPreventionMode.SANITIZE || mode == InjectionPreventionMode.SANITIZE_MARKUP) && ruleDefinition == TestSanitationRules.REPLACE_DOTS
			? "Bad link: https://evil. site"
			: input;

		performTest(input, expectValid, expectedStoredValue);
	}

	/**
	 * Post the given {@code value} to a form and verify expected results.
	 *
	 * <p>
	 *     When {@code expectValid} is true, the response is expected to
	 *     <em>not</em> contain any elements in the {@code error} field.
	 * </p>
	 *
	 * <p>
	 *     The posted data is loaded again and it is verified that the value
	 *     for the input field is the same as {@code expectedStoredValue}.
	 * </p>
	 *
	 * @param value The value to post.
	 * @param expectValid Whether the data is to be accepted by the plugin.
	 * @param expectedStoredValue The expected value that is actually stored.
	 */
	private void performTest(String value, boolean expectValid, String expectedStoredValue) throws IOException {
		String uuid = createForm("inputfield.json");

		RequestBody requestBody = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("input1", value)
			.build();
		okio.Buffer buffer = new okio.Buffer();
		requestBody.writeTo(buffer);

		Request postRequest = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/jsondata"))
			.header("Referer", "ref")
			.post(requestBody)
			.build();
		Response response = client().newCall(postRequest).execute();

		assertThat(response.code())
			.as("Post response code")
			.isEqualTo(HttpResponseStatus.OK.code());

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));

		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);

		JsonObject errors = new JsonObject(response.body().string()).getJsonObject("errors");
		NodeResponse dataNode = dataNodes.getData().get(0);

		if (expectValid) {
			assertThat(errors)
				.as("Post data errors")
				.isNullOrEmpty();

			assertThat(dataNode.getVersion())
				.as("Data Node Version")
				.isEqualTo("1.0");

			assertThat(dataNode.getFields().getStringField("input1").getString())
				.as("Stored data")
				.isEqualTo(expectedStoredValue);
		} else {
			assertThat(errors)
				.as("Post data errors")
				.isNotEmpty();

			assertThat(dataNode.getVersion())
				.as("Data Node Version")
				.isEqualTo("0.1");
		}
	}
}
