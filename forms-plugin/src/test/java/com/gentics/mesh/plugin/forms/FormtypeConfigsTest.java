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
 * Integration tests for the formtypeconfigs endpoints:
 * GET  /{project}/plugins/forms/formtypeconfigs
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
	public void testGetReturnsEmptyObjectInitially() {
		FormtypeConfigsModel result = getFormtypeConfigs(PROJECT_NAME);
		assertThat(result).as("Response").isNotNull();
		assertThat(result.getConfigs()).as("Initially no configs stored").isEmpty();
	}

	@Test
	public void testPostAndGet() throws IOException {
		String body = "{\"poll\":{\"maxChoices\":1},\"generic\":{\"maxElements\":5}}";
		Response postResponse = client().newCall(new Request.Builder()
				.url(projectUrl("/formtypeconfigs"))
				.headers(auth(true))
				.post(RequestBody.create(MediaType.parse("application/json"), body))
				.build()).execute();

		assertThat(postResponse.code()).as("POST status code").isEqualTo(200);

		FormtypeConfigsModel saved = JsonUtil.readValue(postResponse.body().string(), FormtypeConfigsModel.class);
		assertThat(saved.getConfigs()).as("Saved configs contain both keys").containsKeys("poll", "generic");

		FormtypeConfigsModel loaded = getFormtypeConfigs(PROJECT_NAME);
		assertThat(loaded.getConfigs()).as("Loaded configs contain both keys").containsKeys("poll", "generic");
		assertThat(loaded.getConfigs().get("poll").get("maxChoices").asInt()).as("poll.maxChoices").isEqualTo(1);
		assertThat(loaded.getConfigs().get("generic").get("maxElements").asInt()).as("generic.maxElements").isEqualTo(5);
	}

	@Test
	public void testPostOverwritesEntireConfig() throws IOException {
		// store initial config with two keys
		setFormtypeConfigs(PROJECT_NAME, buildModel("{\"poll\":{\"maxChoices\":1},\"generic\":{}}"));

		// overwrite with a config containing only one key
		Response overwriteResponse = setFormtypeConfigs(PROJECT_NAME, buildModel("{\"custom\":{\"someFlag\":true}}"));
		assertThat(overwriteResponse.code()).as("Overwrite POST status code").isEqualTo(200);

		FormtypeConfigsModel loaded = getFormtypeConfigs(PROJECT_NAME);
		assertThat(loaded.getConfigs()).as("Only new key present after overwrite").containsOnlyKeys("custom");
		assertThat(loaded.getConfigs().get("custom").get("someFlag").asBoolean()).as("custom.someFlag").isTrue();
	}

	@Test
	public void testPostEmptyObjectClearsConfig() throws IOException {
		// store something first
		setFormtypeConfigs(PROJECT_NAME, buildModel("{\"poll\":{}}"));

		// clear by posting an empty object
		Response clearResponse = setFormtypeConfigs(PROJECT_NAME, new FormtypeConfigsModel());
		assertThat(clearResponse.code()).as("Clear POST status code").isEqualTo(200);

		FormtypeConfigsModel loaded = getFormtypeConfigs(PROJECT_NAME);
		assertThat(loaded.getConfigs()).as("Configs cleared").isEmpty();
	}

	@Test
	public void testGetWithoutPermissionIsForbidden() throws IOException {
		Response response = client().newCall(new Request.Builder()
				.url(projectUrl("/formtypeconfigs"))
				.headers(auth(false))
				.get()
				.build()).execute();

		assertForbidden(response);
	}

	@Test
	public void testPostWithoutPermissionIsForbidden() throws IOException {
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
