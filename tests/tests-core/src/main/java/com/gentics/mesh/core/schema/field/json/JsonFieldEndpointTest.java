package com.gentics.mesh.core.schema.field.json;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.field.json.JsonFieldTestHelper;
import com.gentics.mesh.core.rest.JsonSchema;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.JsonFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class JsonFieldEndpointTest extends AbstractMeshTest {

	@Test
	public void testResetAllowField() {
		grantAdmin();
		final String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		final String nodeUuid = tx(() -> contentUuid());

		// 1. Update schema and set allowed property
		SchemaModel schema = tx(() -> schemaContainer("content").getLatestVersion().getSchema());
		SchemaUpdateRequest request = JsonUtil.readValue(schema.toJson(), SchemaUpdateRequest.class);
		request.addField(new JsonFieldSchemaImpl()
				.setAllowedSchemas(new JsonSchema("{\"type\":\"object\",\"properties\":{\"firstName\":{\"type\":\"string\"},\"lastName\":{\"type\":\"string\"}},\"required\":[\"firstName\",\"lastName\"]}"))
				.setName("extraJson"));

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, request));
		}, COMPLETED, 1);

		// 2. Update the node slug and expect failure due to now allowed string
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setVersion("draft");
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.getFields().put("extraJson", FieldUtil.createJsonField(JsonFieldTestHelper.make("someValue")));
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest), BAD_REQUEST, "node_error_invalid_json_field_value",
			"extraJson",
			JsonUtil.toJson(JsonFieldTestHelper.make("someValue")));

		// 3. Update the schema again with empty allowed value
		request.removeField("extraJson");
		request.addField(new JsonFieldSchemaImpl()
				.setAllowedSchemas(new JsonSchema("{\"type\":\"object\",\"properties\":{\"content\":{\"type\":\"string\"},\"extra\":{\"type\":\"string\"}},\"required\":[\"content\"]}"))
				.setName("extraJson"));

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, request));
		}, COMPLETED, 1);

		// 4. Update the node again
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest));

	}

}
