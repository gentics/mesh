package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Integration tests for the formtypeconfigs endpoint:
 * POST /{project}/plugins/forms/formtypeconfigs
 */
public class FormtypeConfigsTest extends AbstractFormsPluginIntegrationTest {

	@Before
	@Override
	public void setup() {
		super.setup();
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
	}

	@Test
	public void testSaveFormtypeConfigs() throws IOException {
		Response postResponse = setFormtypeConfigs(PROJECT_NAME, buildModel("{\"poll\":{\"maxChoices\":1},\"generic\":{\"maxElements\":5}}"));

		assertThat(postResponse.code()).as("POST status code").isEqualTo(200);

		FormtypeConfigsModel saved = JsonUtil.readValue(postResponse.body().string(), FormtypeConfigsModel.class);
		assertThat(saved.getConfigs()).as("Saved configs contain both keys").containsKeys("poll", "generic");
		assertThat(saved.getConfigs().get("poll").get("maxChoices").asInt()).as("poll.maxChoices").isEqualTo(1);
		assertThat(saved.getConfigs().get("generic").get("maxElements").asInt()).as("generic.maxElements").isEqualTo(5);
	}

	@Test
	public void testSaveOverwritesEntireConfig() throws IOException {
		setFormtypeConfigs(PROJECT_NAME, buildModel("{\"poll\":{\"maxChoices\":1},\"generic\":{}}"));

		Response overwriteResponse = setFormtypeConfigs(PROJECT_NAME, buildModel("{\"custom\":{\"someFlag\":true}}"));
		assertThat(overwriteResponse.code()).as("Overwrite POST status code").isEqualTo(200);

		FormtypeConfigsModel result = JsonUtil.readValue(overwriteResponse.body().string(), FormtypeConfigsModel.class);
		assertThat(result.getConfigs()).as("Only new key present after overwrite").containsOnlyKeys("custom");
		assertThat(result.getConfigs().get("custom").get("someFlag").asBoolean()).as("custom.someFlag").isTrue();
	}

	@Test
	public void testSaveEmptyConfigClearsExisting() throws IOException {
		setFormtypeConfigs(PROJECT_NAME, buildModel("{\"poll\":{}}"));

		Response clearResponse = setFormtypeConfigs(PROJECT_NAME, new FormtypeConfigsModel());
		assertThat(clearResponse.code()).as("Clear POST status code").isEqualTo(200);

		FormtypeConfigsModel result = JsonUtil.readValue(clearResponse.body().string(), FormtypeConfigsModel.class);
		assertThat(result.getConfigs()).as("Configs cleared").isEmpty();
	}

	@Test
	public void testNoConfigStoredInitially() {
		FormtypeConfigsModel result = FormsTestPlugin.instance.getFormtypeConfigs(PROJECT_NAME, server.client());
		assertThat(result.getConfigs()).as("No formtypeconfigs stored initially").isEmpty();
	}

	@Test
	public void testConfigPersistedInRootNode() throws IOException {
		setFormtypeConfigs(PROJECT_NAME, buildModel("{\"poll\":{\"maxChoices\":1}}"));

		FormtypeConfigsModel result = FormsTestPlugin.instance.getFormtypeConfigs(PROJECT_NAME, server.client());
		assertThat(result.getConfigs()).as("poll key present").containsKey("poll");
		assertThat(result.getConfigs().get("poll").get("maxChoices").asInt()).as("poll.maxChoices").isEqualTo(1);
	}

	@Test
	public void testSaveRequiresAuthentication() throws IOException {
		Response response = client().newCall(new Request.Builder()
				.url(projectUrl("/formtypeconfigs"))
				.headers(auth(false))
				.post(RequestBody.create(MediaType.parse("application/json"), "{}"))
				.build()).execute();

		assertForbidden(response);
	}

	private FormtypeConfigsModel buildModel(String json) throws IOException {
		return JsonUtil.readValue(json, FormtypeConfigsModel.class);
	}
}
