package com.gentics.mesh.core.field.binary;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runners.Parameterized;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.reactivex.Observable;
import io.vertx.core.buffer.Buffer;

/**
 * Mass update test. Depends on the "synchronized writes" option. The tests are considered unstable on sync writes turned off.
 * 
 * @author plyhun
 *
 */
public abstract class AbstractBinaryFieldUploadEndpointParameterizedTest extends AbstractMeshTest {

	@Parameterized.Parameters
	public static Collection<Object> paramData() {
		return IntStream.of(1, 2, 5, 10, 25, 50, 100).boxed().collect(Collectors.toList());
	}

	@Parameterized.Parameter
	public int numUploads;

	public static Optional<Boolean> initialSyncWrites = Optional.empty();

	/**
	 * Should the test enable the synchronization writes for the Mesh instance?
	 * This setting requires the whole Mesh being cold restarted, hence the abstract method, forcing the impls distinction, and not the runtime parameter.
	 * 
	 * @return
	 */
	public abstract boolean isSyncWrites();

	/**
	 * Test parallel upload of the same binary data - thus the same binary vertex should be used.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testParallelDupUpload() throws IOException {
		testParallelUpload(true);
	}

	/**
	 * Test parallel upload of the differently named binary data.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testParallelDiffUpload() throws IOException {
		testParallelUpload(false);
	}

	protected void testParallelUpload(boolean useSameName) throws IOException {
		String folderUuid = tx(() -> folder("news").getUuid());

		// Prepare schema
		try (Tx tx = tx()) {
			prepareTypedSchema(folder("news"), FieldUtil.createBinaryFieldSchema("image"), false);
			tx.success();
		}

		Buffer buffer = getBuffer("/pictures/blume.jpg");

		initialSyncWrites = initialSyncWrites.or(() -> Optional.of(mesh().globalLock().isSyncWrites()));
		getTestContext().getInstanceProvider().setSyncWrites(isSyncWrites());

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
						.updateNodeBinaryField(projectName(), node.getUuid(), "en", node.getVersion(), "image", ins, size, useSameName ? "blume.jpg" : (number + "blume.jpg"), "image/jpeg")
						.toSingle();
				});
		}).lastOrError().ignoreElement().blockingAwait();

		getTestContext().getInstanceProvider().setSyncWrites(initialSyncWrites.get());
	}
}
