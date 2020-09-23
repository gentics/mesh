package com.gentics.mesh.core.schema.field.binary;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.BinaryExtractOptions;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.buffer.Buffer;
import io.vertx.test.core.TestUtils;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class BinaryFieldEndpointTest extends AbstractMeshTest {

	@Test
	public void testBinaryFieldExtractOptions() {
		grantAdmin();
		final String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		final String nodeUuid = tx(() -> contentUuid());
		Buffer buffer = TestUtils.randomBuffer(1000);

		// 1. Update schema and set allowed property
		Schema schema = tx(() -> schemaContainer("content").getLatestVersion().getSchema());
		SchemaUpdateRequest request = JsonUtil.readValue(schema.toJson(), SchemaUpdateRequest.class);
		BinaryExtractOptions extractOptions = new BinaryExtractOptions();
		extractOptions.setContent(true);
		extractOptions.setMetadata(true);
		request.addField(new BinaryFieldSchemaImpl().setBinaryExtractOptions(extractOptions).setName("binaryField"));

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, request));
		}, COMPLETED, 1);

		SchemaResponse schema2 = call(() -> client().findSchemaByUuid(schemaUuid));
		System.out.println(schema2.toJson());
		BinaryFieldSchema binaryField2 = schema2.getField("binaryField", BinaryFieldSchema.class);
		assertNotNull("The options should be set", binaryField2.getBinaryExtractOptions());

		// 2. Update the node binary field
		call(() -> client().updateNodeBinaryField(projectName(), nodeUuid, "en", "draft", "binaryField",
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(),
			"filename.txt", "application/binary"));

		// 3. Update the schema again and remove the extract options. Assert that the schema has been updated
		request.removeField("binaryField");
		request.addField(new BinaryFieldSchemaImpl().setBinaryExtractOptions(null).setName("binaryField"));

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, request));
		}, COMPLETED, 1);

		SchemaResponse schema3 = call(() -> client().findSchemaByUuid(schemaUuid));
		BinaryFieldSchema binaryField3 = schema3.getField("binaryField", BinaryFieldSchema.class);
		assertNull("The options should be set to null", binaryField3.getBinaryExtractOptions());
	}

}
