package com.gentics.mesh.core.schema.field.binary;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.BinaryExtractOptions;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
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
		SchemaModel schema = tx(() -> schemaContainer("content").getLatestVersion().getSchema());
		SchemaUpdateRequest request = JsonUtil.readValue(schema.toJson(), SchemaUpdateRequest.class);

		// 1. Update schema and add field
		{
			BinaryExtractOptions extractOptions = new BinaryExtractOptions();
			extractOptions.setContent(true);
			extractOptions.setMetadata(true);
			request.addField(new BinaryFieldSchemaImpl().setBinaryExtractOptions(extractOptions).setName("binaryField"));

			waitForJobs(() -> {
				call(() -> client().updateSchema(schemaUuid, request));
			}, COMPLETED, 1);

			SchemaResponse loadedSchema = call(() -> client().findSchemaByUuid(schemaUuid));
			BinaryFieldSchema loadedField = loadedSchema.getField("binaryField", BinaryFieldSchema.class);
			assertNotNull("The options should be set", loadedField.getBinaryExtractOptions());
		}

		// 3. Update the node binary field
		Buffer buffer = TestUtils.randomBuffer(1000);
		call(() -> client().updateNodeBinaryField(projectName(), nodeUuid, "en", "draft", "binaryField",
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(),
			"filename.txt", "application/binary"));

		// 2. Update schema and update field (change extract options)
		{
			request.removeField("binaryField");
			BinaryExtractOptions extractOptions = new BinaryExtractOptions();
			extractOptions.setContent(false);
			extractOptions.setMetadata(false);
			request.addField(new BinaryFieldSchemaImpl().setBinaryExtractOptions(extractOptions).setName("binaryField"));

			waitForJobs(() -> {
				call(() -> client().updateSchema(schemaUuid, request));
			}, COMPLETED, 1);

			SchemaResponse loadedSchema = call(() -> client().findSchemaByUuid(schemaUuid));
			BinaryFieldSchema loadedField = loadedSchema.getField("binaryField", BinaryFieldSchema.class);
			BinaryExtractOptions loadedOptions = loadedField.getBinaryExtractOptions();
			assertNotNull("The options should be set", loadedOptions);
			assertFalse(loadedOptions.getContent());
			assertFalse(loadedOptions.getMetadata());
		}

		// 4. Update the schema and update field (remove the extract options). Assert that the schema has been updated
		{
			request.removeField("binaryField");
			request.addField(new BinaryFieldSchemaImpl().setBinaryExtractOptions(null).setName("binaryField"));

			waitForJobs(() -> {
				call(() -> client().updateSchema(schemaUuid, request));
			}, COMPLETED, 1);

			SchemaResponse loadedSchema = call(() -> client().findSchemaByUuid(schemaUuid));
			BinaryFieldSchema loadedField = loadedSchema.getField("binaryField", BinaryFieldSchema.class);
			assertNull("The options should be set to null", loadedField.getBinaryExtractOptions());
		}
	}

}
