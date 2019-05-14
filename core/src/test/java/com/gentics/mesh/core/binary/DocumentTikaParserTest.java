package com.gentics.mesh.core.binary;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.junit.Test;

import io.vertx.core.buffer.Buffer;

public class DocumentTikaParserTest {

	@Test
	public void testTika() throws TikaException, IOException {
		Buffer buffer = getBuffer("/testfiles/test.pdf");
		byte[] data = buffer.getBytes();
		Metadata metadata = new Metadata();
		int limit = 10000;
		String content = DocumentTikaParser.parse(new ByteArrayInputStream(data), metadata, limit).get();
		System.out.println(content);
		System.out.println(metadata.toString());
	}

	protected Buffer getBuffer(String path) throws IOException {
		InputStream ins = getClass().getResourceAsStream(path);
		assertNotNull("The resource for path {" + path + "} could not be found", ins);
		byte[] bytes = IOUtils.toByteArray(ins);
		return Buffer.buffer(bytes);
	}

}
