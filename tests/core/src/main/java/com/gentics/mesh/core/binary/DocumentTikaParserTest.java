package com.gentics.mesh.core.binary;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.junit.Ignore;
import org.junit.Test;

import io.vertx.core.buffer.Buffer;

public class DocumentTikaParserTest {

	public static final int LIMIT = 40000;

	@Test
	public void testTika() throws TikaException, IOException {
		Buffer buffer = getBuffer("/testfiles/test.pdf");
		byte[] data = buffer.getBytes();
		Metadata metadata = new Metadata();
		String content = DocumentTikaParser.parse(new ByteArrayInputStream(data), metadata, LIMIT).get();
		System.out.println(content);
		System.out.println(metadata.toString());
	}

	protected Buffer getBuffer(String path) throws IOException {
		InputStream ins = getClass().getResourceAsStream(path);
		assertNotNull("The resource for path {" + path + "} could not be found", ins);
		byte[] bytes = IOUtils.toByteArray(ins);
		return Buffer.buffer(bytes);
	}

	@Test
	@Ignore
	public void testFilesInFolder() throws TikaException, IOException {
		File folder = new File("/media/ext4/tmp/dbfiles");

		for (int i = 0; i < 2; i++) {
			for (File file : folder.listFiles()) {
				System.out.println("Testing: " + file.getName());
				try {
					testParser(file);
				} catch (Throwable e) {
					e.printStackTrace();
					System.in.read();
				}
			}
		}

		System.out.println("Now testing PDF");
		testParser(new File("/media/ext4/tmp/SUP-10413.pdf"));
	}

	private void testParser(File file) throws TikaException, IOException {
		FileInputStream fis = new FileInputStream(file);
		try {
			Metadata metadata = new Metadata();
			Optional<String> op = DocumentTikaParser.parse(fis, metadata, LIMIT);
			if (op.isPresent()) {
				String content = op.get();
				System.out.println(content);
				System.out.println(metadata.toString());
			}
		} finally {
			fis.close();
		}
	}

}
