package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.info.BranchSchemaInfo;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.forms.model.schema.FormData;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.plugin.forms.service.SchemaService;

import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Test cases for migration of old binary fields to the new structure
 */
@RunWith(Parameterized.class)
public class FormBinaryFieldMigrationTest extends AbstractFormPostDataTest {
	public final static int WAIT_TIMEOUT_MS = 5_000;

	/**
	 * File Uploads for testing
	 */
	public final static List<String> FILE_UPLOADS = List.of("file-uploads/test.jpg", "file-uploads/test.json",
			"file-uploads/test.odt", "file-uploads/test.pdf", "file-uploads/test.png", "file-uploads/test.txt",
			"file-uploads/test.zip");

	/**
	 * Condition for binary items
	 */
	public final static Condition<NodeResponse> BINARY_ITEM = new Condition<>(
			node -> Strings.CS.equals(node.getSchema().getName(), SchemaInitializer.MULTIBINARY_ITEM_SCHEMA_NAME),
			"binary item node");

	@Parameters(name = "{index}: single field {0}, modified {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean singleField : List.of(true, false)) {
			for (boolean modified : List.of(true, false)) {
				data.add(new Object[] { singleField, modified });
			}
		}
		return data;
	}

	protected String formUuid;

	/**
	 * Map of UUID -> filename of old data nodes (created before the migration)
	 */
	protected Map<String, String> oldDataNodes = new HashMap<>();

	/**
	 * Map of UUId -> filename of new data nodes (created after the migration)
	 */
	protected Map<String, String> newDataNodes = new HashMap<>();

	/**
	 * Map of UUID -> filename of all data nodes
	 */
	protected Map<String, String> allDataNodes = new HashMap<>();

	/**
	 * True when the form should contain a single binary field, false for multiple
	 */
	@Parameter(0)
	public boolean singleField;

	/**
	 * True when the form should be modified after the migration
	 */
	@Parameter(1)
	public boolean modified;

	@Before
	public void setup() {
		super.setup();
		String formBefore = singleField ? "migration_single_field_before.json" : "migration_multi_field_before.json";
		String formAfter = singleField ? "migration_single_field_after.json" : "migration_multi_field_after.json";
		String formModified = singleField ? "migration_single_field_modified.json" : "migration_multi_field_modified.json";

		try {
			// create original form
			formUuid = createForm(formBefore);
			String dataFolderUuid = getDataFolder(formUuid).getUuid();
			String schemaName = "form_%s".formatted(formUuid);

			// modify the schema by adding a binary field(s) (so that the form looks like it would with the old version)
			Optional<SchemaResponse> optSchema = call(() -> server.client().findSchemas()).getData().stream()
					.filter(schema -> Strings.CI.equals(schema.getName(), schemaName)).findFirst();
			assertThat(optSchema).as("Schema").isPresent();

			String schemaUuid = optSchema.get().getUuid();

			SchemaUpdateRequest updateSchema = optSchema.get().toUpdateRequest();
			updateSchema.addField(new BinaryFieldSchemaImpl().setName("file1"));

			if (!singleField) {
				updateSchema.addField(new BinaryFieldSchemaImpl().setName("file2"));
			}

			call(() -> server.client().updateSchema(schemaUuid, updateSchema));

			// get the form data folder
			NodeListResponse formNodeChildren = call(() -> server.client().findNodeChildren(PROJECT_NAME, formUuid));
			Optional<NodeResponse> optDataFolder = formNodeChildren.getData().stream()
					.filter(n -> Strings.CI.equals(n.getDisplayName(), FormService.DATA_FOLDER_NAME)).findFirst();
			assertThat(optDataFolder).as("Form Data Folder").isNotEmpty();

			// post some data (old style)
			for (String fileName : FILE_UPLOADS) {
				String dataNodeUuid = postFormDataOldStyle(dataFolderUuid, schemaName, "Posted file: %s".formatted(fileName), fileName);
				oldDataNodes.put(dataNodeUuid, fileName);
			}

			// update the form (adding the proper binary field(s)). This should trigger the migration
			JsonNode updateResponse = doRequest(new Request.Builder()
					.url(projectUrl("/forms/" + formUuid))
					.headers(auth(true))
					.post(requestBodyFromFile(formAfter))
					.build(), true, true, JsonNode.class);

			assertThat(updateResponse).as("Response").isNotNull();
			assertThat(updateResponse.get("uuid").asText()).as("Returned uuid").isEqualTo(formUuid);

			// wait until the form schema has been migrated for the default branch
			waitForMigration(formUuid);

			// post some data (new style)
			for (String fileName : FILE_UPLOADS) {
				String dataNodeUuid = postFormDataNewStyle(formUuid, "Posted file (new): %s".formatted(fileName), fileName);
				newDataNodes.put(dataNodeUuid, fileName);
			}

			allDataNodes.putAll(oldDataNodes);
			allDataNodes.putAll(newDataNodes);

			// optionally modify the form by adding another field
			if (modified) {
				updateResponse = doRequest(new Request.Builder()
					.url(projectUrl("/forms/" + formUuid))
					.headers(auth(true))
					.post(requestBodyFromFile(formModified))
					.build(), true, true, JsonNode.class);

				assertThat(updateResponse).as("Response").isNotNull();
				assertThat(updateResponse.get("uuid").asText()).as("Returned uuid").isEqualTo(formUuid);

				// wait until the form schema has been migrated for the default branch
				waitForMigration(formUuid);
			}
		} catch (IOException | InterruptedException e) {
			fail("Error while preparing test data", e);
		}
	}

	/**
	 * Test that the report will contain the data of the binary field (for old an new data alike)
	 * @throws IOException
	 */
	@Test
	public void testReport() throws IOException {
		String url = "/forms/%s/data?publishedOnly=false".formatted(formUuid);

		Request reportRequest = new Request.Builder()
				.url(projectUrl(url))
				.headers(auth(true))
				.get()
				.build();
		Response reportResponse = client().newCall(reportRequest).execute();
		assertThat(reportResponse.isSuccessful()).as(String.format("Response success %d (%s)", reportResponse.code(), reportResponse.message())).isTrue();

		JsonNode reportData = JsonUtil.getMapper().readTree(reportResponse.body().string());
		ArrayNode entries = reportData.withArrayProperty("entries");

		Map<String, String> reportedData = new HashMap<>();

		for (JsonNode entry : entries) {
			String uuid = entry.findPath("uuid").asText();
			String fileName = entry.findPath("fields").findPath("file1").findPath("binary").findPath("fileName").asText();
			reportedData.put(uuid, fileName);
		}

		assertThat(reportedData).as("Reported data").containsExactlyInAnyOrderEntriesOf(allDataNodes);
	}

	/**
	 * Test that the CSV Export will contain the data of the binary field (for old an new data alike)
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testExport() throws IOException {
		Set<String> expectedRecords = new HashSet<>();
		// add header
		if (modified) {
			if (singleField) {
				expectedRecords.add("input1,input2,file1,file1 (filename)");
			} else {
				expectedRecords.add("input1,input2,file1,file1 (filename),file2,file2 (filename)");
			}
		} else {
			if (singleField) {
				expectedRecords.add("input1,file1,file1 (filename)");
			} else {
				expectedRecords.add("input1,file1,file1 (filename),file2,file2 (filename)");
			}
		}
		// add old records
		for (Map.Entry<String, String> entry : oldDataNodes.entrySet()) {
			String dataNodeUuid = entry.getKey();
			String fileName = entry.getValue();
			if (modified) {
				if (singleField) {
					expectedRecords.add("Posted file: %s,,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file1,%s"
							.formatted(fileName, formUuid, dataNodeUuid, fileName));
				} else {
					expectedRecords.add("Posted file: %s,,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file1,%s,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file2,%s"
							.formatted(fileName, formUuid, dataNodeUuid, fileName, formUuid, dataNodeUuid, fileName));
				}
			} else {
				if (singleField) {
					expectedRecords.add("Posted file: %s,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file1,%s"
							.formatted(fileName, formUuid, dataNodeUuid, fileName));
				} else {
					expectedRecords.add("Posted file: %s,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file1,%s,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file2,%s"
							.formatted(fileName, formUuid, dataNodeUuid, fileName, formUuid, dataNodeUuid, fileName));
				}
			}
		}

		// add new records
		for (Map.Entry<String, String> entry : newDataNodes.entrySet()) {
			String dataNodeUuid = entry.getKey();
			String fileName = entry.getValue();
			if (modified) {
				if (singleField) {
					expectedRecords.add("Posted file (new): %s,,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file1,%s"
							.formatted(fileName, formUuid, dataNodeUuid, fileName));
				} else {
					expectedRecords.add("Posted file (new): %s,,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file1,%s,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file2,%s"
							.formatted(fileName, formUuid, dataNodeUuid, fileName, formUuid, dataNodeUuid, fileName));
				}
			} else {
				if (singleField) {
					expectedRecords.add("Posted file (new): %s,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file1,%s"
							.formatted(fileName, formUuid, dataNodeUuid, fileName));
				} else {
					expectedRecords.add("Posted file (new): %s,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file1,%s,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file2,%s"
							.formatted(fileName, formUuid, dataNodeUuid, fileName, formUuid, dataNodeUuid, fileName));
				}
			}
		}

		// download the export
		String url = "/forms/%s/export?publishedOnly=false".formatted(formUuid);

		Request exportRequest = new Request.Builder()
				.url(projectUrl(url))
				.headers(auth(true))
				.get()
				.build();
		Response exportResponse = client().newCall(exportRequest).execute();
		assertThat(exportResponse.isSuccessful()).as(String.format("Response success %d (%s)", exportResponse.code(), exportResponse.message())).isTrue();
		Set<String> actualRecords = getExportRecords(exportResponse.body().string());

		// check export contents
		assertThat(actualRecords).as("Export").hasSameElementsAs(expectedRecords);
	}

	/**
	 * Test that the binary URLs for downloading the binary data work for old an new data alike
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testBinaryURLs() throws IOException {
		for (Map.Entry<String, String> entry : oldDataNodes.entrySet()) {
			String dataNodeUuid = entry.getKey();
			String fileName = entry.getValue();
			assertBinaryDownload(formUuid, dataNodeUuid, "file1", fileName);
			if (!singleField) {
				assertBinaryDownload(formUuid, dataNodeUuid, "file2", fileName);
			}
		}
		for (Map.Entry<String, String> entry : newDataNodes.entrySet()) {
			String dataNodeUuid = entry.getKey();
			String fileName = entry.getValue();
			assertBinaryDownload(formUuid, dataNodeUuid, "file1", fileName);
			if (!singleField) {
				assertBinaryDownload(formUuid, dataNodeUuid, "file2", fileName);
			}
		}
	}

	/**
	 * Test the the binary zip contains the uploaded files for old an new data alike
	 * @throws IOException
	 */
	@Test
	@Ignore
	public void testBinariesZip() throws IOException {
		String url = "/forms/%s/binaries".formatted(formUuid);

		Request zipDownloadRequest = new Request.Builder()
				.url(projectUrl(url))
				.headers(auth(true))
				.get()
				.build();
		Response zipDownloadResponse = client().newCall(zipDownloadRequest).execute();

		assertThat(zipDownloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", zipDownloadResponse.code(), zipDownloadResponse.message())).isTrue();

		Map<String, String> foundFiles = new HashMap<>();
		try (ZipInputStream zipIn = new ZipInputStream(zipDownloadResponse.body().byteStream())) {
			ZipEntry entry = null;
			while ((entry = zipIn.getNextEntry()) != null) {
				String fullName = entry.getName();
				String[] parts = StringUtils.split(fullName, "/", 3);

				String dataNodeUuid = parts[0];
				String name = parts[2];

				foundFiles.put(dataNodeUuid, name);
				StringWriter fileContents = new StringWriter();
				IOUtils.copy(zipIn, fileContents, "UTF-8");
				assertThat(fileContents.toString()).as(String.format("Contents of file %s", name)).isEqualTo(fromFile(name));
			}
		}

		assertThat(foundFiles).as("Exported Files").containsExactlyInAnyOrderEntriesOf(allDataNodes);
	}

	/**
	 * Post data to the form in the old style (binary form field is represented by a binary field in the schema).
	 * This method will not actually use the form plugin, but will directly create the data node
	 * @param dataFolderUuid UUID of the data folder
	 * @param schemaName schema name
	 * @param inputData data for the field input1
	 * @param postedFileName filename of the posted file
	 * @return data node UUID
	 * @throws IOException
	 */
	protected String postFormDataOldStyle(String dataFolderUuid, String schemaName, String inputData,
			String postedFileName) throws IOException {
		NodeCreateRequest create = new NodeCreateRequest();
		create.setLanguage("en");
		create.setParentNodeUuid(dataFolderUuid);
		create.setSchemaName(schemaName);
		create.getFields().put("input1", new StringFieldImpl().setString(inputData));

		NodeResponse createdNode = call(() -> server.client().createNode(PROJECT_NAME, create));

		Map<String, Object> fileData = getFileData(postedFileName);
		long fileSize = (long) (fileData.get("size"));
		String fileName = (String) fileData.get("fileName");
		String fileType = (String) fileData.get("type");

		try (InputStream in = getClass().getResourceAsStream(postedFileName)) {
			call(() -> server.client().updateNodeBinaryField(PROJECT_NAME, createdNode.getUuid(), "en", "draft",
					"file1", in, fileSize, fileName, fileType));
		}
		if (!singleField) {
			try (InputStream in = getClass().getResourceAsStream(postedFileName)) {
				call(() -> server.client().updateNodeBinaryField(PROJECT_NAME, createdNode.getUuid(), "en", "draft",
						"file2", in, fileSize, fileName, fileType));
			}
		}

		return createdNode.getUuid();
	}

	/**
	 * Post data to the form in the new style (binary form field is represented by a node field in the schema).
	 * @param formUuid form UUID
	 * @param inputData data for the field input1
	 * @param postedFileName filename of the posted file
	 * @return data node UUID
	 * @throws IOException
	 */
	protected String postFormDataNewStyle(String formUuid, String inputData, String postedFileName) throws IOException {
		String url = "/form/%s/save?lang=en".formatted(formUuid);

		Map<String, Object> formData = null;
		if (singleField) {
			formData = Map.of("file1", getFileData(postedFileName), "input1", inputData);
		} else {
			formData = Map.of("file1", getFileData(postedFileName), "file2", getFileData(postedFileName), "input1", inputData);
		}

		MultipartBody.Builder requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart(fileUploadKey("file1"), postedFileName, requestBodyFromFile(postedFileName))
				.addFormDataPart("formdata", makeFormdataMultipart(formUuid, null, formData));

		if (!singleField) {
			requestBody = requestBody.addFormDataPart(fileUploadKey("file2"), postedFileName,
					requestBodyFromFile(postedFileName));
		}

		Request postRequest = new Request.Builder().url(projectUrl(url)).header("Referer", "ref").post(requestBody.build()).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		return JsonUtil.readValue(response.body().string(), FormData.class).getFormDataID();
	}

	/**
	 * Wait until the schema for the form is fully migrated in the default branch of the project
	 * @param formUuid form UUID
	 * @throws InterruptedException
	 */
	protected void waitForMigration(String formUuid) throws InterruptedException {
		String schemaName = SchemaService.getSchemaName(formUuid);

		Optional<BranchResponse> optLatestBranch = call(() -> server.client().findBranches(PROJECT_NAME)).getData().stream()
				.filter(branch -> BooleanUtils.isTrue(branch.getLatest())).findFirst();
		assertThat(optLatestBranch).as("Latest branch").isPresent();

		boolean isMigrated = isSchemaMigrated(schemaName, optLatestBranch.get().getUuid());
		long start = System.currentTimeMillis();
		while (!isMigrated) {
			long waitTime = System.currentTimeMillis() - start;
			if (waitTime > WAIT_TIMEOUT_MS) {
				fail("Schema %s failed to be migrated withing %d ms".formatted(schemaName, WAIT_TIMEOUT_MS));
			}
			Thread.sleep(1000);
			isMigrated = isSchemaMigrated(schemaName, optLatestBranch.get().getUuid());
		}
	}

	/**
	 * Check whether the schema with given name is fully migrated in the given branch
	 * @param schemaName schema name
	 * @param branchUuid branch UUID
	 * @return true if migrated, false if not
	 */
	protected boolean isSchemaMigrated(String schemaName, String branchUuid) {
		Optional<BranchSchemaInfo> optSchemaInfo = call(
				() -> server.client().getBranchSchemaVersions(PROJECT_NAME, branchUuid)).getSchemas().stream()
				.filter(schema -> Strings.CS.equals(schema.getName(), schemaName)).findFirst();
		assertThat(optSchemaInfo).as("Schema in Branch").isPresent();

		return optSchemaInfo.get().getMigrationStatus() == JobStatus.COMPLETED;
	}

	/**
	 * Use the forms plugin to download a binary and check whether it has correct content
	 * @param formUuid form UUID
	 * @param dataNodeUuid data node UUID
	 * @param fieldName field name
	 * @param fileName filename
	 * @throws IOException
	 */
	protected void assertBinaryDownload(String formUuid, String dataNodeUuid, String fieldName, String fileName) throws IOException {
		Request downloadRequest = new Request.Builder()
				.url(projectUrl("/forms/%s/data/%s/binary/%s".formatted(formUuid, dataNodeUuid, fieldName)))
				.headers(auth(true))
				.get()
				.build();
		Response downloadResponse = client().newCall(downloadRequest).execute();

		assertThat(downloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", downloadResponse.code(), downloadResponse.message())).isTrue();
		String download = downloadResponse.body().string();
		assertThat(download).as("Downloaded file").isEqualTo(fromFile(fileName));
	}
}
