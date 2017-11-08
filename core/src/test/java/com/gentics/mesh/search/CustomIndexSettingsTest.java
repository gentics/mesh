package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.IndexOptionHelper;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true)
public class CustomIndexSettingsTest extends AbstractNodeSearchEndpointTest {

	/**
	 * Test the validation behaviour. Schema updates which include bogus json should fail early with a meaningful message.
	 */
	@Test
	public void testValidationErrorOnCreate() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("settingsTest");
		request.setSearchIndex(new JsonObject().put("somebogus", "value"));
		request.addField(FieldUtil.createStringFieldSchema("text").setSearchIndex(new JsonObject().put("bogus", "value")));
		call(() -> client().createSchema(request), BAD_REQUEST, "schema_error_index_validation",
				"Failed to parse mapping [node]: illegal field [bogus], only fields can be specified inside fields");
	}

	@Test
	public void testValidationErrorOnUpdate() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("settingsTest");
		request.setSearchIndex(new JsonObject().put("somebogus", "value"));
		request.addField(FieldUtil.createStringFieldSchema("text").setSearchIndex(IndexOptionHelper.getRawFieldOption()));
		SchemaResponse response = call(() -> client().createSchema(request));

		SchemaUpdateRequest updateRequest = JsonUtil.readValue(request.toJson(), SchemaUpdateRequest.class);
		updateRequest.removeField("text");
		updateRequest.addField(FieldUtil.createStringFieldSchema("text").setSearchIndex(new JsonObject().put("bogus", "value")));
		call(() -> client().updateSchema(response.getUuid(), updateRequest), BAD_REQUEST, "schema_error_index_validation",
				"Failed to parse mapping [node]: illegal field [bogus], only fields can be specified inside fields");
	}

	@Test
	public void testSuccessfulValidation() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("settingsTest");
		request.setSearchIndex(new JsonObject().put("somebogus", "value"));
		request.addField(FieldUtil.createStringFieldSchema("text").setSearchIndex(IndexOptionHelper.getRawFieldOption()));
		call(() -> client().createSchema(request));
	}

	/**
	 * Verify that the schema gets updated if only the index settings have been altered.
	 */
	@Test
	public void testSchemaDiff() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("settingsTest");
		request.setSearchIndex(new JsonObject().put("somebogus", "value"));
		request.addField(FieldUtil.createStringFieldSchema("text").setSearchIndex(IndexOptionHelper.getRawFieldOption()));
		SchemaResponse response = call(() -> client().createSchema(request));

		SchemaUpdateRequest updateRequest = JsonUtil.readValue(request.toJson(), SchemaUpdateRequest.class);
		updateRequest.setSearchIndex(new JsonObject().put("somebogus", "value2"));
		call(() -> client().updateSchema(response.getUuid(), updateRequest));

		SchemaResponse response2 = call(() -> client().findSchemaByUuid(response.getUuid()));
		assertEquals("value2", response2.getSearchIndex().getString("somebogus"));
		assertNotEquals("The schema should have been updated by the introduced change but it was not.", response.getVersion(), response2
				.getVersion());

		updateRequest.setSearchIndex(new JsonObject());
		call(() -> client().updateSchema(response.getUuid(), updateRequest));

		SchemaResponse response3 = call(() -> client().findSchemaByUuid(response.getUuid()));
		assertTrue("The options should be empty", new JsonObject().equals(response3.getSearchIndex()));
		assertNotEquals("The schema should have been updated by the introduced change but it was not.", response2.getVersion(), response3
				.getVersion());
	}
}
