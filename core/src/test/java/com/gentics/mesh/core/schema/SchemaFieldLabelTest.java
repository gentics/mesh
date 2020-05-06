package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class SchemaFieldLabelTest extends AbstractMeshTest {

	private SchemaResponse schema;

	@Before
	public void setUp() throws Exception {
		SchemaCreateRequest schemaCreateRequest = new SchemaCreateRequest();
		schemaCreateRequest.setName("testSchema");
		schemaCreateRequest.setDescription("initialDescription");
		schemaCreateRequest.setFields(Arrays.asList(
			new StringFieldSchemaImpl()
				.setName("testField")
				.setLabel("initialLabel")
		));

		schema = client().createSchema(schemaCreateRequest).blockingGet();
	}

	@Test
	public void changeLabel() {
		GenericMessageResponse response = updateSchema(field -> field.put("label", "updatedLabel"));
		assertThat(response).matches("schema_updated_migration_invoked", "testSchema", "2.0");
	}

	@Test
	public void sameLabel() {
		GenericMessageResponse response = updateSchema(field -> field.put("label", "initialLabel"));
		assertThat(response).matches("schema_update_no_difference_detected");
	}

	@Test
	public void setLabelToNull() {
		GenericMessageResponse response = updateSchema(field -> field.put("label", (JsonObject) null));
		assertThat(response).matches("schema_update_no_difference_detected");
	}

	@Test
	public void omitLabel() {
		GenericMessageResponse response = updateSchema(field -> {});
		assertThat(response).matches("schema_update_no_difference_detected");
	}

	public GenericMessageResponse updateSchema(Consumer<JsonObject> fieldChanger) {
		// We can't use the MeshRestClient because we have to set a json property to null, which is not possible with the MeshRestClient.
		String response;
		try {
			JsonObject field = new JsonObject()
				.put("name", "testField")
				.put("type", "string");

			fieldChanger.accept(field);

			response = httpClient().newCall(new Request.Builder()
				.url(String.format("http://localhost:%d%s/schemas/%s", port(), VersionHandler.CURRENT_API_BASE_PATH, schema.getUuid()))
				.addHeader("Authorization", "Bearer " + client().getAuthentication().getToken())
				.post(RequestBody.create(MediaType.parse("application/json"), new JsonObject()
					.put("name", "testSchema")
					.put("fields", new JsonArray().add(field))
					.encode()
				))
				.build()
			).execute().body().string();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return JsonUtil.readValue(response, GenericMessageResponse.class);
	}
}
