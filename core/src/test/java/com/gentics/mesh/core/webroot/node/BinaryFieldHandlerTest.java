package com.gentics.mesh.core.webroot.node;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.endpoint.node.BinaryFieldHandler;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.storage.LocalBinaryStorage;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.madl.tx.Tx;

import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.ext.web.FileUpload;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class BinaryFieldHandlerTest extends AbstractMeshTest {

	private BinaryFieldHandler handler;

	private MeshUploadOptions uploadOptions;

	@Before
	public void setup() throws Exception {
		uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
		handler = meshDagger().nodeFieldAPIHandler();
		File uploadFolder = getUploadFolder();
		FileUtils.deleteDirectory(uploadFolder);
	}

	@After
	public void cleanup() throws IOException {
		File uploadFolder = getUploadFolder();
		FileUtils.deleteDirectory(uploadFolder);
	}

	@Test
	public void testFileUploadHandler() throws IOException {
		File uploadFolder = getUploadFolder();
		try (Tx tx = tx()) {
			// Setup schema
			prepareSchema(content(), null, "binaryField");

			// Mock upload request
			InternalActionContext ac = mockContext(mockUpload("blibbla"));
			CaseInsensitiveHeaders attributes = new CaseInsensitiveHeaders();
			attributes.add("language", "en");
			attributes.add("version", "1.0");

			// Assert initial condition
			assertFalse("Initially no upload folder should exist.", uploadFolder.exists());
			assertNull("Initially no binary field should be found.", content().getLatestDraftFieldContainer(english()).getBinary("binaryField"));

			// Invoke request
			handler.handleUpdateField(ac, contentUuid(), "binaryField", attributes);

			// Assert result
			BinaryGraphField field = content().getLatestDraftFieldContainer(english()).getBinary("binaryField");
			assertEquals("Filename did not match.", "bla", field.getFileName());
			assertEquals("Size of the file did not match.", 7, field.getBinary().getSize());
			assertEquals("mimetype did not match.", "text/plain", field.getMimeType());
			String uuid = field.getBinary().getUuid();
			String path = LocalBinaryStorage.getFilePath(uuid);
			assertTrue("The file should be placed in the local binary storage.", new File(path).exists());
		}

		try (Tx tx = tx()) {
			// Clear upload folder for next upload
			FileUtils.deleteDirectory(uploadFolder);
			assertThat(uploadFolder).as("The upload folder should have been created").doesNotExist();

			// Prepare request
			InternalActionContext ac2 = mockContext(mockUpload("blub123"));
			CaseInsensitiveHeaders attributes = new CaseInsensitiveHeaders();
			attributes.add("language", "en");
			attributes.add("version", "1.1");

			// Invoke second upload
			handler.handleUpdateField(ac2, contentUuid(), "binaryField", attributes);

			// Assert result
			BinaryGraphField field = content().getLatestDraftFieldContainer(english()).getBinary("binaryField");
			String uuid = field.getBinary().getUuid();
			String path = LocalBinaryStorage.getFilePath(uuid);
			assertTrue("The file should be placed in the local binary storage.", new File(path).exists());
			assertTrue("The upload folder {" + uploadFolder.getAbsolutePath() + "} should have been created.", uploadFolder.exists());
		}
	}

	@Test
	public void testHandlerCase2() throws IOException {
		File uploadFolder = getUploadFolder();
		try (Tx tx = tx()) {
			// Setup the schema & request
			prepareSchema(content(), null, "binaryField");
			CaseInsensitiveHeaders attributes = new CaseInsensitiveHeaders();
			attributes.add("language", "en");
			attributes.add("version", "1.0");
			InternalActionContext ac = mockContext(mockUpload("blub123"));

			// Assert initial condition
			assertFalse("Initially no upload folder should exist.", uploadFolder.exists());

			// Invoke the request
			handler.handleUpdateField(ac, contentUuid(), "binaryField", attributes);

			// Assert result
			assertThat(uploadFolder).as("The upload folder should have been created").exists();
		}
	}

	@Test(expected = GenericRestException.class)
	public void testFileUploadWithNoUploadFile() throws Throwable {
		try (Tx tx = tx()) {
			// Setup the schema & request
			prepareSchema(content(), null, "binaryField");
			FileUpload upload = mockUpload("blub123");
			InternalActionContext ac = mockContext(upload);
			CaseInsensitiveHeaders attributes = new CaseInsensitiveHeaders();
			attributes.add("language", "en");
			attributes.add("version", "1.0");

			// Delete the file on purpose in order to invoke an error
			new File(upload.uploadedFileName()).delete();

			// Invoke the request
			handler.handleUpdateField(ac, contentUuid(), "binaryField", attributes);
		}
	}

	private File getUploadFolder() {
		return new File(uploadOptions.getDirectory());
	}

	private FileUpload mockUpload(String content) throws IOException {
		FileUtils.forceDeleteOnExit(new File(uploadOptions.getDirectory()));
		File sourceFile = new File("target/testfile_" + System.currentTimeMillis());
		sourceFile.deleteOnExit();
		sourceFile.createNewFile();
		FileUtils.writeStringToFile(sourceFile, content);

		FileUpload fileUpload = mock(FileUpload.class);
		when(fileUpload.fileName()).thenReturn("bla");
		when(fileUpload.contentType()).thenReturn("text/plain");
		when(fileUpload.size()).thenReturn((long) content.length());
		when(fileUpload.uploadedFileName()).thenReturn(sourceFile.getAbsolutePath());
		return fileUpload;
	}

	private InternalActionContext mockContext(FileUpload fileUpload) {
		AtomicReference<Object> file = new AtomicReference<>();
		InternalActionContext context = mock(InternalActionContext.class);

		when(context.getFileUploads()).thenReturn(new HashSet<FileUpload>(Arrays.asList(fileUpload)));
		when(context.getProject()).thenReturn(project());
		when(context.getUser()).thenReturn(getRequestUser());
		when(context.getBranch()).thenReturn(initialBranch());
		when(context.get("sourceFile")).thenAnswer(answer -> file.get());
		when(context.put(eq("sourceFile"), anyObject())).thenAnswer(answer -> {
			file.set(answer.getArgumentAt(1, Object.class));
			return context;
		});

		return context;
	}
}
