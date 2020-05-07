package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class SchemaOptionalUpdateTest extends AbstractMeshTest {

	private SchemaResponse schema;

	@Before
	public void setUp() throws Exception {
		SchemaCreateRequest schemaCreateRequest = new SchemaCreateRequest()
			.setName("testSchema")
			.setDescription("initialDescription")
			.setDisplayField("testField")
			.setSegmentField("testField")
			.setUrlFields(Arrays.asList("testField"))
			.setAutoPurge(true)
			.setContainer(true)
			.setElasticsearch(getJson("/elasticsearch/custom/suggestionSettings.json"));

		schemaCreateRequest.setFields(Arrays.asList(
			new StringFieldSchemaImpl()
				.setName("testField")
				.setLabel("initialLabel")
				.setRequired(true)
				.setElasticsearch(getJson("/elasticsearch/custom/suggestionFieldMapping.json")),
			new StringFieldSchemaImpl()
				.setName("testField2")
		));

		schema = client().createSchema(schemaCreateRequest).blockingGet();
	}

	@Parameterized.Parameters(name = "{0}")
	public static List<PropertyChange> changes() {
		return Arrays.asList(
			new PropertyChange("description", "initialDescription", "changedDescription", false),
			new PropertyChange("displayField", "testField", "testField2", false),
			new PropertyChange("segmentField", "testField", "testField2", false),
			new PropertyChange("urlFields", Arrays.asList("testField"), Arrays.asList("testField2"), false),
			new PropertyChange("autoPurge", true, false, false),
			new PropertyChange("container", true, false, false),
			new PropertyChange("elasticsearch", staticJson("/elasticsearch/custom/suggestionSettings.json"), new JsonObject(), false),
			new PropertyChange("label", "initialLabel", "changedLabel", true),
			new PropertyChange("required", true, false, true),
			new PropertyChange("elasticsearch", staticJson("/elasticsearch/custom/suggestionFieldMapping.json"), new JsonObject(), true)
		);
	}

	private static JsonObject staticJson(String path) {
		try {
			return new JsonObject(IOUtils.toString(SchemaOptionalUpdateTest.class.getResourceAsStream(path)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Parameterized.Parameter
	public PropertyChange propertyChange;

	@Test
	public void changeProperty() {
		JsonObject request = createSchemaUpdateRequest();

		propertyChange.getTargetObject(request)
			.put(propertyChange.property, propertyChange.changedValue);

		GenericMessageResponse response = updateSchema(request);
		assertThat(response).matches("schema_updated_migration_invoked", "testSchema", "2.0");
	}

	@Test
	public void sameProperty() {
		JsonObject request = createSchemaUpdateRequest();

		propertyChange.getTargetObject(request)
			.put(propertyChange.property, propertyChange.initialValue);

		GenericMessageResponse response = updateSchema(request);
		assertThat(response).matches("schema_update_no_difference_detected");
	}

	@Test
	public void setPropertyToNull() {
		JsonObject request = createSchemaUpdateRequest();

		propertyChange.getTargetObject(request)
			.put(propertyChange.property, (JsonObject) null);

		GenericMessageResponse response = updateSchema(request);
		assertThat(response).matches("schema_update_no_difference_detected");
	}

	@Test
	public void omitProperty() {
		GenericMessageResponse response = updateSchema(createSchemaUpdateRequest());
		assertThat(response).matches("schema_update_no_difference_detected");
	}

	public GenericMessageResponse updateSchema(JsonObject schemaRequest) {
		// We can't use the MeshRestClient because we have to set a json property to null, which is not possible with the MeshRestClient.
		String response;
		try {
			response = httpClient().newCall(new Request.Builder()
				.url(String.format("http://localhost:%d%s/schemas/%s", port(), VersionHandler.CURRENT_API_BASE_PATH, schema.getUuid()))
				.addHeader("Authorization", "Bearer " + client().getAuthentication().getToken())
				.post(RequestBody.create(MediaType.parse("application/json"), schemaRequest.encode()))
				.build()
			).execute().body().string();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return JsonUtil.readValue(response, GenericMessageResponse.class);
	}

	private JsonObject createSchemaUpdateRequest() {
		return new JsonObject()
			.put("name", "testSchema")
			.put("fields", new JsonArray().add(new JsonObject()
				.put("name", "testField")
				.put("type", "string")));
	}

	private static class PropertyChange {
		private final String property;
		private final Object initialValue;
		private final Object changedValue;
		private final Boolean fieldProperty;

		private PropertyChange(String property, Object initialValue, Object changedValue, Boolean fieldProperty) {
			this.property = property;
			this.initialValue = initialValue;
			this.changedValue = changedValue;
			this.fieldProperty = fieldProperty;
		}

		private JsonObject getTargetObject(JsonObject schema) {
			if (fieldProperty) {
				return schema.getJsonArray("fields").getJsonObject(0);
			} else {
				return schema;
			}
		}

		@Override
		public String toString() {
			String prefix = fieldProperty
				? "field."
				: "schema.";
			return prefix + property;
		}
	}
}
