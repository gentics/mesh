package com.gentics.mesh.core.webroot.node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import com.gentics.mesh.context.InternalActionContext;
import io.vertx.core.file.FileSystemException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.verticle.node.BinaryFieldHandler;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.ext.web.FileUpload;
import rx.exceptions.CompositeException;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeFieldAPIHandlerTest extends AbstractMeshTest {

	private BinaryFieldHandler handler;

	private MeshUploadOptions uploadOptions;

	final String data = "bliblablub";
	final String hash = "406d7d8188bb4556f7616628d1a5cd281ef6686034ddb3855b0ebb6affe6675e8ba9cde8f60f183341a0105223533e1ca09570e5d024cc8173d0b5087dfab4b5";
	String segmentedPath = "some/path/to/file";

	@Before
	public void setup() throws Exception {
		uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
		handler = meshDagger().nodeFieldAPIHandler();
	}

	@Test
	public void testFileUploadHandler() throws IOException {

		InternalActionContext ac = mockContext();
		File uploadFolder = getUploadFolder();
		String fileUpload = mockUpload();

		ac.put("sourceFile", fileUpload);
		assertFalse("Initially no upload folder should exist.", uploadFolder.exists());
		handler.moveBinaryFile(ac, UUIDUtil.randomUUID(), segmentedPath);
		assertFalse("The upload file should have been moved.", new File(fileUpload).exists());
		assertThat(uploadFolder).as("The upload folder should have been created").exists();
		FileUtils.deleteDirectory(uploadFolder);

		fileUpload = mockUpload();
		ac.put("sourceFile", fileUpload);
		assertThat(uploadFolder).as("The upload folder should have been created").doesNotExist();
		handler.moveBinaryFile(ac, UUIDUtil.randomUUID(), segmentedPath);
		assertFalse("The upload file should have been moved.", new File(fileUpload).exists());
		assertTrue("The upload folder should have been created.", uploadFolder.exists());
	}

	@Test
	public void testHandlerCase2() throws IOException {
		segmentedPath = "/cdfb/34f9/598a/4173/bb34/f959/8ae1/7330/";
		InternalActionContext ac = mockContext();
		String fileUpload = mockUpload();
		ac.put("sourceFile", fileUpload);
		File uploadFolder = getUploadFolder();
		assertFalse("Initially no upload folder should exist.", uploadFolder.exists());

		handler.moveBinaryFile(ac, UUIDUtil.randomUUID(), segmentedPath);
		assertFalse("The upload file should have been moved.", new File(fileUpload).exists());
		assertThat(uploadFolder).as("The upload folder should have been created").exists();
		FileUtils.deleteDirectory(uploadFolder);
	}

	@Test(expected = FileSystemException.class)
	public void testFileUploadWithNoUploadFile() throws Throwable {
		InternalActionContext ac = mockContext();
		String fileUpload = mockUpload();
		ac.put("sourceFile", fileUpload);

		// Delete the file on purpose in order to invoke an error
		new File(fileUpload).delete();
		try {
			handler.moveBinaryFile(ac, UUIDUtil.randomUUID(), segmentedPath);
		} catch (CompositeException e) {
			throw e.getExceptions().get(1);
		}
	}

	private File getUploadFolder() {
		return new File(uploadOptions.getDirectory(), segmentedPath);
	}

	private String mockUpload() throws IOException {

		FileUtils.forceDeleteOnExit(new File(uploadOptions.getDirectory()));
		File sourceFile = new File("target/testfile_" + System.currentTimeMillis());
		sourceFile.deleteOnExit();
		sourceFile.createNewFile();
		FileUtils.writeStringToFile(sourceFile, data);

		FileUpload fileUpload = mock(FileUpload.class);
		when(fileUpload.fileName()).thenReturn("bla");
		when(fileUpload.uploadedFileName()).thenReturn(sourceFile.getAbsolutePath());
		return sourceFile.getAbsolutePath();
	}

	private InternalActionContext mockContext() {
		AtomicReference<Object> file = new AtomicReference<>();
		InternalActionContext context = mock(InternalActionContext.class);

		when(context.get("sourceFile")).thenAnswer(answer -> file.get());
		when(context.put(eq("sourceFile"), anyObject())).thenAnswer(answer -> {
			file.set(answer.getArgumentAt(1, Object.class));
			return context;
		});

		return context;
	}
}
