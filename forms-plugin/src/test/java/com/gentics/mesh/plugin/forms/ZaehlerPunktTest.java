package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.forms.model.PostDataResponse;

import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Variation test for various different configurations and values of the Zählerpunkt field.
 * Each tests posts a form and tests the validation of that field.
 */
@RunWith(Parameterized.class)
public class ZaehlerPunktTest extends AbstractFormsPluginIntegrationTest {

	private NodeResponse formNode;

	private final String formJson;
	private final String value;
	private final boolean shouldBeValid;

	@Parameterized.Parameters(name = "{index}: {0}, value: \"{1}\"")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"zaehlerpunkt.json", "12345678", true},
			{"zaehlerpunkt.json", "123", false},
			{"zaehlerpunkt.json", "", true},
			{"zaehlerpunkt.json", "null", false},
			{"zaehlerpunkt.json", "undefined", false},
			{"zaehlerpunkt_mandatory.json", "12345678", true},
			{"zaehlerpunkt_mandatory.json", "123", false},
			{"zaehlerpunkt_mandatory.json", "", false},
			{"zaehlerpunkt_mandatory.json", "null", false},
			{"zaehlerpunkt_mandatory.json", "undefined", false},
			{"zaehlerpunkt_withZaehlerpunkt.json", "12345678", true},
			{"zaehlerpunkt_withZaehlerpunkt.json", "123", false},
			{"zaehlerpunkt_withZaehlerpunkt.json", "", true},
			{"zaehlerpunkt_withZaehlerpunkt.json", "null", false},
			{"zaehlerpunkt_withZaehlerpunkt.json", "undefined", false},
			{"zaehlerpunkt_withZaehlerpunkt_mandatory.json", "12345678", true},
			{"zaehlerpunkt_withZaehlerpunkt_mandatory.json", "123", false},
			{"zaehlerpunkt_withZaehlerpunkt_mandatory.json", "", false},
			{"zaehlerpunkt_withZaehlerpunkt_mandatory.json", "null", false},
			{"zaehlerpunkt_withZaehlerpunkt_mandatory.json", "undefined", false},
		});
	}

	public ZaehlerPunktTest(String formJson, String value, boolean shouldBeValid) {
		this.formJson = formJson;
		this.value = value;
		this.shouldBeValid = shouldBeValid;
	}

	@Override
	@Before
	public void setup() {
		super.setup();
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();

		try {
			String uuid = createForm(formJson);
			formNode = server.client().findNodeByUuid(PROJECT_NAME, uuid).blockingGet();
		} catch (IOException e) {
			fail("Setup of form failed", e);
		}
	}

	@Test
	public void testZaehlerpunkt() throws IOException {
		MultipartBody.Builder builder = new MultipartBody.Builder()
			.setType(MultipartBody.FORM);

		builder.addFormDataPart("input11", "");
		builder.addFormDataPart("zaehlerpunkt30", value);

		RequestBody requestBody = builder.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + formNode.getUuid() + "/jsondata")).post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		PostDataResponse responsePOJO = JsonUtil.readValue(response.body().string(), PostDataResponse.class);
		if (shouldBeValid) {
			assertThat(responsePOJO.getErrors())
				.as("Checking errors: %s", JsonUtil.toJson(responsePOJO.getErrors()))
				.isEmpty();
		} else {
			assertThat(responsePOJO.getErrors())
				.as("Checking errors: %s", JsonUtil.toJson(responsePOJO.getErrors()))
				.isNotEmpty();
		}
	}
}
