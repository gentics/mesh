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
import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.core.binary.BinaryDataProcessorContext;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;

import dagger.Lazy;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;

public class TikaBinaryProcessorTest {

	@Test
	public void tikaCachingTest() throws FileNotFoundException, IOException {
		Lazy<Vertx> lazy = Mockito.mock(Lazy.class);
		when(lazy.get()).thenReturn(Vertx.vertx());
		TikaBinaryProcessor processor = new TikaBinaryProcessor(lazy, new MeshOptions(), mockDb());
		FileUpload ul = mockUpload("test.pdf", "application/pdf");

		Maybe<Consumer<BinaryGraphField>> result = processor.process(new BinaryDataProcessorContext(null, null, null, ul, "HASHSUM"));

		Consumer<BinaryGraphField> consumer = result.blockingGet();
		BinaryGraphField field = Mockito.mock(BinaryGraphField.class);
		consumer.accept(field);
	}

	private Database mockDb() {
		Database mock = mock(Database.class);
		// This is to shortcut the check if the field needs to be parsed.
		when(mock.singleTxWriteLock(any())).thenReturn(Single.just(true));
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
