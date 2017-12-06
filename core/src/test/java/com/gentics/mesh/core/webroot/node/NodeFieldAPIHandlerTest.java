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
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.verticle.node.BinaryFieldHandler;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.storage.LocalBinaryStorage;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.file.FileSystemException;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.ext.web.FileUpload;

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
		try (Tx tx = tx()) {
			prepareSchema(content(), null, "binaryField");
			FileUpload fileUpload = mockUpload();
			File uploadFolder = getUploadFolder();
			InternalActionContext ac = mockContext(fileUpload);

			ac.put("sourceFile", fileUpload);
			assertFalse("Initially no upload folder should exist.", uploadFolder.exists());
			CaseInsensitiveHeaders attributes = new CaseInsensitiveHeaders();
			attributes.add("language", "en");
			attributes.add("version", "1.0");
			assertNull("Initially no binary field should be found.", content().getLatestDraftFieldContainer(english()).getBinary("binaryField"));
			handler.handleUpdateField(ac, contentUuid(), "binaryField", attributes);
			BinaryGraphField field = content().getLatestDraftFieldContainer(english()).getBinary("binaryField");
			assertEquals("bla", field.getFileName());
			assertEquals(0, field.getBinary().getSize());
			assertEquals("text/plain", field.getMimeType());

			String uuid = field.getBinary().getUuid();
			String path = ((LocalBinaryStorage) MeshInternal.get().binaryStorage()).getFilePath(uuid);
			assertTrue("The file should be placed in the local binary storage.", new File(path).exists());

			assertFalse("The upload file should have been moved.", new File(fileUpload.uploadedFileName()).exists());
			assertThat(uploadFolder).as("The upload folder should have been created").exists();
			FileUtils.deleteDirectory(uploadFolder);

			fileUpload = mockUpload();
			ac.put("sourceFile", fileUpload);
			assertThat(uploadFolder).as("The upload folder should have been created").doesNotExist();
			handler.handleUpdateField(ac, contentUuid(), "binaryField", null);
			assertFalse("The upload file should have been moved.", new File(fileUpload.uploadedFileName()).exists());
			assertTrue("The upload folder should have been created.", uploadFolder.exists());
		}
	}

	@Test
	public void testHandlerCase2() throws IOException {
		try (Tx tx = tx()) {
			prepareSchema(content(), null, "binaryField");
			segmentedPath = "/cdfb/34f9/598a/4173/bb34/f959/8ae1/7330/";
			FileUpload fileUpload = mockUpload();
			InternalActionContext ac = mockContext(fileUpload);
			ac.put("sourceFile", fileUpload);
			File uploadFolder = getUploadFolder();
			assertFalse("Initially no upload folder should exist.", uploadFolder.exists());

			CaseInsensitiveHeaders attributes = new CaseInsensitiveHeaders();
			attributes.add("language", "en");
			attributes.add("version", "1.0");
			handler.handleUpdateField(ac, contentUuid(), "binaryField", attributes);
			assertFalse("The upload file should have been moved.", new File(fileUpload.uploadedFileName()).exists());
			assertThat(uploadFolder).as("The upload folder should have been created").exists();
			FileUtils.deleteDirectory(uploadFolder);
		}
	}

	@Test(expected = FileSystemException.class)
	public void testFileUploadWithNoUploadFile() throws Throwable {
		try (Tx tx = tx()) {
			prepareSchema(content(), null, "binaryField");
			FileUpload fileUpload = mockUpload();
			InternalActionContext ac = mockContext(fileUpload);
			ac.put("sourceFile", fileUpload);

			// Delete the file on purpose in order to invoke an error
			new File(fileUpload.uploadedFileName()).delete();
			CaseInsensitiveHeaders attributes = new CaseInsensitiveHeaders();
			attributes.add("language", "en");
			attributes.add("version", "1.0");
			handler.handleUpdateField(ac, contentUuid(), "binaryField", attributes);
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
		when(fileUpload.contentType()).thenReturn("text/plain");
		when(fileUpload.uploadedFileName()).thenReturn(sourceFile.getAbsolutePath());
		return fileUpload;
	}

	private InternalActionContext mockContext(FileUpload fileUpload) {
		AtomicReference<Object> file = new AtomicReference<>();
		InternalActionContext context = mock(InternalActionContext.class);

		when(context.getFileUploads()).thenReturn(new HashSet<FileUpload>(Arrays.asList(fileUpload)));
		when(context.getProject()).thenReturn(project());
		when(context.getUser()).thenReturn(getRequestUser());
		when(context.getRelease()).thenReturn(initialRelease());
		when(context.get("sourceFile")).thenAnswer(answer -> file.get());
		when(context.put(eq("sourceFile"), anyObject())).thenAnswer(answer -> {
			file.set(answer.getArgumentAt(1, Object.class));
			return context;
		});

		return context;
	}
}
