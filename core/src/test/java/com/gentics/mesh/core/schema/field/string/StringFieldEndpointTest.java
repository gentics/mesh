package com.gentics.mesh.core.schema.field.string;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class StringFieldEndpointTest extends AbstractMeshTest {

	@Test
	public void testResetAllowField() {
		grantAdminRole();
		final String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		final String nodeUuid = tx(() -> contentUuid());

		// 1. Update schema and set allowed property
		Schema schema = tx(() -> schemaContainer("content").getLatestVersion().getSchema());
		SchemaUpdateRequest request = JsonUtil.readValue(schema.toJson(), SchemaUpdateRequest.class);
		request.addField(new StringFieldSchemaImpl().setAllowedValues("a", "b", "c").setName("extraString"));

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, request));
		}, COMPLETED, 1);

		// 2. Update the node slug and expect failure due to now allowed string
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setVersion("draft");
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.getFields().put("extraString", FieldUtil.createStringField("someValue"));
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest), BAD_REQUEST, "node_error_invalid_string_field_value",
			"extraString",
			"someValue");

		// 3. Update the schema again with empty allowed value
		request.removeField("extraString");
		request.addField(new StringFieldSchemaImpl().setAllowedValues().setName("extraString"));

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, request));
		}, COMPLETED, 1);

		// 4. Update the node again
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest));

	}

}
