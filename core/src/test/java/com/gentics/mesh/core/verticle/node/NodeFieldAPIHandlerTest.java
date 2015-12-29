package com.gentics.mesh.core.verticle.node;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.ext.web.FileUpload;

public class NodeFieldAPIHandlerTest extends AbstractDBTest {

	@Autowired
	private NodeFieldAPIHandler handler;

	private MeshUploadOptions uploadOptions;
	final String data = "bliblablub";
	final String hash = "CF83E1357EEFB8BDF1542850D66D8007D620E4050B5715DC83F4A921D36CE9CE47D0D13C5D85F2B0FF8318D2877EEC2F63B931BD47417A81A538327AF927DA3E";
	final String segmentedPath = "some/path/to/file";

	@Before
	public void setup() {
		uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
	}

	@Test
	public void testFileUploadHandler() throws IOException {

		FileUpload fileUpload = mockUpload();
		File uploadFolder = getUploadFolder();
		assertFalse("Initially no upload folder should exist.", uploadFolder.exists());

		String hashOutput = handler.hashAndMoveBinaryFile(fileUpload, UUIDUtil.randomUUID(), segmentedPath).toBlocking().last();
		assertNotNull(hashOutput);
		assertEquals("The generated hash did not out expected value for data {" + data + "}", hash, hashOutput);
		assertFalse("The upload file should have been moved.", new File(fileUpload.uploadedFileName()).exists());
		assertThat(uploadFolder).as("The upload folder should have been created").exists();
		FileUtils.deleteDirectory(uploadFolder);

		fileUpload = mockUpload();
		assertThat(uploadFolder).as("The upload folder should have been created").doesNotExist();
		hashOutput = handler.hashAndMoveBinaryFile(fileUpload, UUIDUtil.randomUUID(), segmentedPath).toBlocking().last();
		assertNotNull(hashOutput);
		assertEquals("The generated hash did not out expected value for data {" + data + "}", hash, hashOutput);
		assertFalse("The upload file should have been moved.", new File(fileUpload.uploadedFileName()).exists());
		assertTrue("The upload folder should have been created.", uploadFolder.exists());
	}

	@Test(expected = HttpStatusCodeErrorException.class)
	public void testFileUploadWithNoUploadFile() throws IOException {
		FileUpload fileUpload = mockUpload();
		// Delete the file on purpose in order to invoke an error
		new File(fileUpload.uploadedFileName()).delete();
		handler.hashAndMoveBinaryFile(fileUpload, UUIDUtil.randomUUID(), segmentedPath).toBlocking().last();
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
