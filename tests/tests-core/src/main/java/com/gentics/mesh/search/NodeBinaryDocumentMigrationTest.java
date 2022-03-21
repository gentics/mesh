package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.buffer.Buffer;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeBinaryDocumentMigrationTest extends AbstractMultiESTest {

	public NodeBinaryDocumentMigrationTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void schemaMigrationWithDocumentBinary() {
		grantAdmin();
		uploadDocumentNode();
		waitForJobs((Runnable) this::migrateSchema, JobStatus.COMPLETED, 1);
	}

	private void uploadDocumentNode() {
		NodeResponse node = createBinaryContent().blockingGet();
		Buffer buffer = Buffer.buffer("This is a text");
		client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), node.getLanguage(), node.getVersion(),
			"binary", new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "text.txt", "text/plain").blockingAwait();
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
