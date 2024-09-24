package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.binary.BinaryMetadata;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.buffer.Buffer;

/**
 * Test cases for parsing and storing metadata of binaries
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class BinaryMetadataParseTest extends AbstractMeshTest {
	/**
	 * Test uploading an image, that contains entries in the metadata with names, that only differ in case (Text_TextEntry vs. tEXt_tEXtEntry)
	 * @throws IOException
	 */
	@Test
	public void testUploadWithCaseSensitiveMetadata() throws IOException {
		String parentNodeUuid = tx(() -> project().getBaseNode().getUuid());

		Buffer buffer = getBuffer("/binaries/GEN_Logo_RGB_341px.png");
		NodeResponse node = createBinaryNode(parentNodeUuid);
		NodeResponse nodeResponse = call(
			() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), "en", "0.1", "binary", new ByteArrayInputStream(buffer.getBytes()),
				buffer.length(), "GEN_Logo_RGB_341px.png", "image/png"));

		BinaryField binaryField = nodeResponse.getFields().getBinaryField("binary");
		assertThat(binaryField).as("binary field").isNotNull();
		BinaryMetadata metadata = binaryField.getMetadata();
		assertThat(metadata).as("meta data").isNotNull();
		assertThat(metadata.getMap()).as("meta data").containsKeys("Text_TextEntry", "tEXt_tEXtEntry");
	}
}
