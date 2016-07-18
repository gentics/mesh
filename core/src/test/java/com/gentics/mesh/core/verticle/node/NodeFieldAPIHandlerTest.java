package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.ext.web.FileUpload;
import rx.exceptions.CompositeException;

public class NodeFieldAPIHandlerTest extends AbstractDBTest {

	@Autowired
	private NodeFieldAPIHandler handler;

	private MeshUploadOptions uploadOptions;
	final String data = "bliblablub";
	final String hash = "406d7d8188bb4556f7616628d1a5cd281ef6686034ddb3855b0ebb6affe6675e8ba9cde8f60f183341a0105223533e1ca09570e5d024cc8173d0b5087dfab4b5";
	String segmentedPath = "some/path/to/file";

	@Before
	public void setup() {
		uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
	}

	@Test
	public void testFileUploadHandler() throws IOException {

		FileUpload fileUpload = mockUpload();
		File uploadFolder = getUploadFolder();
		assertFalse("Initially no upload folder should exist.", uploadFolder.exists());

		String hashOutput = handler.hashAndMoveBinaryFile(fileUpload, UUIDUtil.randomUUID(), segmentedPath).toBlocking().value();
		assertNotNull(hashOutput);
		assertEquals("The generated hash did not out expected value for data {" + data + "}", hash, hashOutput);
		assertFalse("The upload file should have been moved.", new File(fileUpload.uploadedFileName()).exists());
		assertThat(uploadFolder).as("The upload folder should have been created").exists();
		FileUtils.deleteDirectory(uploadFolder);

		fileUpload = mockUpload();
		assertThat(uploadFolder).as("The upload folder should have been created").doesNotExist();
		hashOutput = handler.hashAndMoveBinaryFile(fileUpload, UUIDUtil.randomUUID(), segmentedPath).toBlocking().value();
		assertNotNull(hashOutput);
		assertEquals("The generated hash did not out expected value for data {" + data + "}", hash, hashOutput);
		assertFalse("The upload file should have been moved.", new File(fileUpload.uploadedFileName()).exists());
		assertTrue("The upload folder should have been created.", uploadFolder.exists());
	}

	@Test
	public void testHandlerCase2() throws IOException {
		segmentedPath = "/cdfb/34f9/598a/4173/bb34/f959/8ae1/7330/";
		FileUpload fileUpload = mockUpload();
		File uploadFolder = getUploadFolder();
		assertFalse("Initially no upload folder should exist.", uploadFolder.exists());

		String hashOutput = handler.hashAndMoveBinaryFile(fileUpload, UUIDUtil.randomUUID(), segmentedPath).toBlocking().value();
		assertNotNull(hashOutput);
		assertEquals("The generated hash did not out expected value for data {" + data + "}", hash, hashOutput);
		assertFalse("The upload file should have been moved.", new File(fileUpload.uploadedFileName()).exists());
		assertThat(uploadFolder).as("The upload folder should have been created").exists();
		FileUtils.deleteDirectory(uploadFolder);
	}

	@Test(expected = GenericRestException.class)
	public void testFileUploadWithNoUploadFile() throws Throwable {
		FileUpload fileUpload = mockUpload();
		// Delete the file on purpose in order to invoke an error
		new File(fileUpload.uploadedFileName()).delete();
		try {
			handler.hashAndMoveBinaryFile(fileUpload, UUIDUtil.randomUUID(), segmentedPath).toBlocking().value();
		} catch (CompositeException e) {
			throw e.getExceptions().get(1);
		}
	}

	private File getUploadFolder() {
		return new File(uploadOptions.getDirectory(), segmentedPath);
	}

	private FileUpload mockUpload() throws IOException {

		FileUtils.forceDeleteOnExit(new File(uploadOptions.getDirectory()));
		File sourceFile = new File("target/testfile_" + System.currentTimeMillis());
		sourceFile.deleteOnExit();
		sourceFile.createNewFile();
		FileUtils.writeStringToFile(sourceFile, data);

		FileUpload fileUpload = mock(FileUpload.class);
		when(fileUpload.fileName()).thenReturn("bla");
		when(fileUpload.uploadedFileName()).thenReturn(sourceFile.getAbsolutePath());
		return fileUpload;
	}

}
