package com.gentics.mesh.core.schema.versioning;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class SchemaNodeVersioningEndpointTest extends AbstractMeshTest {

	@Test
	public void testDisableVersioning() {
		disableVersionedFlag();

		String nodeUuid = contentUuid();
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("draft");
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new name"));
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest));
		nodeUpdateRequest.getFields().put("slug", FieldUtil.createStringField("new name22"));
		call(() -> client().updateNode(projectName(), nodeUuid, nodeUpdateRequest));
	}

	private void disableVersionedFlag() {
		grantAdminRole();
		String contentSchemaUuid = tx(() -> schemaContainer("content").getUuid());
		assertThat(call(() -> client().findSchemaByUuid(contentSchemaUuid))).isVersioned();
		waitForJobs(() -> {
			SchemaResponse schema = call(() -> client().findSchemaByUuid(contentSchemaUuid));
			SchemaUpdateRequest updateRequest = schema.toUpdateRequest();
			updateRequest.setVersioned(false);
			call(() -> client().updateSchema(contentSchemaUuid, updateRequest));
		}, COMPLETED, 1);
		assertThat(call(() -> client().findSchemaByUuid(contentSchemaUuid))).isNotVersioned();
	}

}
