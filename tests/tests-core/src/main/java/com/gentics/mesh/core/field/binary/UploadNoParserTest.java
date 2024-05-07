package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.MeshCoreOptionChanger.NO_UPLOAD_PARSER;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.buffer.Buffer;

/**
 * Test upload processing with disabled parser.
 */
@MeshTestSetting(testSize = FULL, startServer = true, optionChanger = NO_UPLOAD_PARSER)
public class UploadNoParserTest extends AbstractMeshTest {

	@Test
	public void testUploadFilesForTika() throws IOException {
		String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());

		List<String> files = Arrays.asList("small.mp4", "small.ogv", "test.pdf", "test.docx");
		for (String file : files) {
			Buffer buffer = getBuffer("/testfiles/" + file);
			NodeResponse node = createBinaryNode(parentNodeUuid);
			NodeResponse node2 = call(
				() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()),
					buffer.length(), file, "application/pdf"));
			BinaryField binaryField = node2.getFields().getBinaryField("binary");
			assertNull("No plain text should have been extracted", binaryField.getPlainText());
			assertTrue("No metadata should be extracted {" + file + "}", binaryField.getMetadata().getMap().isEmpty());
		}

	}

	@Test
	public void testUploadExif() throws IOException {
		String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());
		Buffer buffer = getBuffer("/pictures/android-gps.jpg");
		NodeResponse node = createBinaryNode(parentNodeUuid);
		call(() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()),
			buffer.length(), "test.jpg", "image/jpeg"));

		NodeResponse node2 = call(() -> client().findNodeByUuid(PROJECT_NAME, node.getUuid()));
		BinaryField binaryField = node2.getFields().getBinaryField("binary");
		BinaryMetadata metadata2 = binaryField.getMetadata();
		assertTrue("No metadata should be extracted", metadata2.getMap().isEmpty());
		assertNull("The no content should be extracted.", binaryField.getPlainText());

		NodeUpdateRequest nodeUpdateRequest = node2.toRequest();
		BinaryField field = nodeUpdateRequest.getFields().getBinaryField("binary");
		field.getMetadata().clear();
		//field.getMetadata().add("dummy", "value");
		nodeUpdateRequest.getFields().put("binary", field);
		NodeResponse node3 = call(() -> client().updateNode(PROJECT_NAME, node.getUuid(), nodeUpdateRequest));

		BinaryMetadata metadata3 = node3.getFields().getBinaryField("binary").getMetadata();
		assertTrue("No metadata should be extracted", metadata3.getMap().isEmpty());

		// Upload the image again and check that the metadata will be updated
		NodeResponse node4 = call(
			() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", node3.getVersion(), "binary",
				new ByteArrayInputStream(buffer.getBytes()), buffer.length(), "test.jpg", "image/jpeg"));
		BinaryMetadata metadata4 = node4.getFields().getBinaryField("binary").getMetadata();
		assertTrue("No metadata should be extracted", metadata4.getMap().isEmpty());
	}
}
