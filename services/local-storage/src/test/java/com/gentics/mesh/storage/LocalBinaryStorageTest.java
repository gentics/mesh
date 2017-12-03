package com.gentics.mesh.storage;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.tx.Tx;

public class LocalBinaryStorageTest {

//	@Test
//	public void testPathSegmentation() throws IOException {
//		Node node = folder("news");
//		try (Tx tx = tx()) {
//			node.setUuid(UUIDUtil.randomUUID());
//			// Add some test data
//			prepareSchema(node, "", "binary");
//			tx.success();
//		}
//		String contentType = "application/octet-stream";
//		String fileName = "somefile.dat";
//		int binaryLen = 10000;
//		call(() -> uploadRandomData(node, "en", "binary", binaryLen, contentType, fileName));
//		try (Tx tx = tx()) {
//			// Load the uploaded binary field and return the segment path to the field
//			BinaryGraphField binaryField = node.getLatestDraftFieldContainer(english()).getBinary("binary");
//			String uuid = "b677504736ed47a1b7504736ed07a14a";
//			binaryField.setUuid(uuid);
//			String path = binaryField.getSegmentedPath();
//			assertEquals("/b677/5047/36ed/47a1/b750/4736/ed07/a14a/", path);
//		}
//	}
}
