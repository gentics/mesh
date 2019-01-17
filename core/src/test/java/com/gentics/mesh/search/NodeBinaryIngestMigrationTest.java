package com.gentics.mesh.search;

import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import io.vertx.core.buffer.Buffer;
import org.junit.Test;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = true, testSize = FULL, startServer = true, withIngestPlugin = true)
public class NodeBinaryIngestMigrationTest extends AbstractMeshTest {

	@Test
	public void schemaMigrationWithIngestableBinary() {
		grantAdminRole();
		uploadIngestableNode();
		waitForJobs(this::migrateSchema, MigrationStatus.COMPLETED, 1);
	}

	private void uploadIngestableNode() {
		NodeResponse node = createBinaryContent().blockingGet();
		client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), node.getLanguage(), node.getVersion(),
			"binary", Buffer.buffer("This is a text"), "text.txt", "text/plain").blockingAwait();
	}

	private void migrateSchema() {
		SchemaListResponse schemas = client().findSchemas().toSingle().blockingGet();
		SchemaResponse schema = schemas.getData().stream()
			.filter(s -> s.getName().equals("binary_content"))
			.findFirst().get();

		SchemaUpdateRequest request = schema.toUpdateRequest();
		request.getFields().add(new StringFieldSchemaImpl().setName("some_field"));

		client().updateSchema(schema.getUuid(), request).blockingAwait();
	}


}
