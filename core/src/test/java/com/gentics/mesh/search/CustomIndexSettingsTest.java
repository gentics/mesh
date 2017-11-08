package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

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
}
