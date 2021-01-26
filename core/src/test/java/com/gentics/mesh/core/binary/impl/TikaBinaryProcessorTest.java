package com.gentics.mesh.core.binary.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.MeshOptionsTypeUnawareContext;
import com.gentics.mesh.core.binary.BinaryDataProcessorContext;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import io.reactivex.Maybe;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;

public class TikaBinaryProcessorTest implements MeshOptionsTypeUnawareContext {

	@Test
	public void tikaCachingTest() throws FileNotFoundException, IOException {
		Lazy<Vertx> lazy = Mockito.mock(Lazy.class);
		when(lazy.get()).thenReturn(Vertx.vertx());
		TikaBinaryProcessor processor = new TikaBinaryProcessor(lazy, getOptions(), mockDb());
		FileUpload ul = mockUpload("test.pdf", "application/pdf");

		Maybe<Consumer<BinaryGraphField>> result = processor.process(new BinaryDataProcessorContext(null, null, null, ul, "HASHSUM"));

		Consumer<BinaryGraphField> consumer = result.blockingGet();
		BinaryGraphField field = Mockito.mock(BinaryGraphField.class);
		consumer.accept(field);
	}

	@Test
	@Ignore
	public void testFilesInFolder() throws IOException {
		File folder = new File("/media/ext4/tmp/dbfiles");

		Lazy<Vertx> lazy = Mockito.mock(Lazy.class);
		when(lazy.get()).thenReturn(Vertx.vertx());
		TikaBinaryProcessor processor = new TikaBinaryProcessor(lazy, getOptions(), mockDb());

		for (int i = 0; i < 2; i++) {
			for (File file : folder.listFiles()) {
				System.out.println("Testing: " + file.getName());
				try {
					testParser(processor, file);
				} catch (Throwable e) {
					e.printStackTrace();
					System.in.read();
				}
			}
		}

		System.out.println("Now testing PDF");
		testParser(processor, new File("/media/ext4/tmp/SUP-10413.pdf"));
	}

	private void testParser(TikaBinaryProcessor processor, File file) {
		FileUpload ul = mock(FileUpload.class);
		when(ul.uploadedFileName()).thenReturn(file.getAbsolutePath());
		when(ul.contentType()).thenReturn("application/pdf");

		Maybe<Consumer<BinaryGraphField>> result = processor.process(new BinaryDataProcessorContext(null, null, null, ul, "HASHSUM"));

		Consumer<BinaryGraphField> consumer = result.blockingGet();
		BinaryGraphField field = Mockito.mock(BinaryGraphField.class);
		consumer.accept(field);
	}

	private Database mockDb() {
		Database mock = mock(Database.class);
		// This is to shortcut the check if the field needs to be parsed.
		when(mock.maybeTx(any())).thenReturn(Maybe.empty());
		return mock;
	}

	private FileUpload mockUpload(String name, String contentType) throws FileNotFoundException, IOException {
		FileUpload ul = mock(FileUpload.class);
		File target = new File("target", "testupload.pdf");
		IOUtils.copy(getClass().getResourceAsStream("/testfiles/" + name), new FileOutputStream(target));
		when(ul.uploadedFileName()).thenReturn(target.getAbsolutePath());
		when(ul.contentType()).thenReturn(contentType);
		return ul;
	}
}
