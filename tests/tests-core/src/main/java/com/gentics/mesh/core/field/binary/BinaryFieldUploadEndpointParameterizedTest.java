package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.reactivex.Observable;
import io.vertx.core.buffer.Buffer;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class BinaryFieldUploadEndpointParameterizedTest extends AbstractMeshTest {

	@Parameterized.Parameters(name = "{index}")
	public static Collection<Object> paramData() {
		return IntStream.of(1,2,5,10,25,50,100,200).boxed().collect(Collectors.toList());
	}

	@Parameterized.Parameter
	public int numUploads;

	/**
	 * Test parallel upload of the same binary data - thus the same binary vertex should be used.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testParallelDupUpload() throws IOException {

		String folderUuid = tx(() -> folder("news").getUuid());

		// Prepare schema
		try (Tx tx = tx()) {
			prepareTypedSchema(folder("news"), FieldUtil.createBinaryFieldSchema("image"), false);
			tx.success();
		}

		Buffer buffer = getBuffer("/pictures/blume.jpg");
		Observable.range(0, numUploads).flatMapSingle(number -> {
			NodeCreateRequest request = new NodeCreateRequest();
			request.setLanguage("en");
			request.setParentNodeUuid(folderUuid);
			request.setSchemaName("folder");
			request.getFields().put("slug", FieldUtil.createStringField("folder" + number));
			return client().createNode(PROJECT_NAME, request).toSingle()
				.flatMap(node -> {
					byte[] data = buffer.getBytes();
					int size = data.length;
					InputStream ins = new ByteArrayInputStream(data);
					return client()
						.updateNodeBinaryField(projectName(), node.getUuid(), "en", node.getVersion(), "image", ins, size, "blume.jpg", "image/jpeg")
						.toSingle();
				});
		}).lastOrError().ignoreElement().blockingAwait();

	}
}
