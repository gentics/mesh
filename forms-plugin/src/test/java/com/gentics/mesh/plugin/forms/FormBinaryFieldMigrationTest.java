package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Strings;
import org.assertj.core.api.Condition;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
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

public class FormBinaryFieldMigrationTest extends AbstractFormPostDataTest {
	public final static int WAIT_TIMEOUT_MS = 5_000;

	public final static List<String> FILE_UPLOADS = List.of("file-uploads/test.jpg", "file-uploads/test.json",
			"file-uploads/test.odt", "file-uploads/test.pdf", "file-uploads/test.png", "file-uploads/test.txt",
			"file-uploads/test.zip");;

	public final static Condition<NodeResponse> BINARY_ITEM = new Condition<>(
			node -> Strings.CS.equals(node.getSchema().getName(), SchemaInitializer.MULTIBINARY_ITEM_SCHEMA_NAME),
			"binary item node");

	@Test
	public void testMigrateSingleField() throws IOException, InterruptedException {
		// create a form with a single input field
		String formUuid = createForm("migration_single_field_before.json");
		String dataFolderUuid = getDataFolder(formUuid).getUuid();
		String schemaName = "form_%s".formatted(formUuid);

		// modify the schema by adding a binary field (so that the form looks like it would with the old version)
		Optional<SchemaResponse> optSchema = call(() -> server.client().findSchemas()).getData().stream()
				.filter(schema -> Strings.CI.equals(schema.getName(), schemaName)).findFirst();
		assertThat(optSchema).as("Schema").isPresent();

		String schemaUuid = optSchema.get().getUuid();

		SchemaUpdateRequest updateSchema = optSchema.get().toUpdateRequest();
		updateSchema.addField(new BinaryFieldSchemaImpl().setName("file1"));

		call(() -> server.client().updateSchema(schemaUuid, updateSchema));

		// get the form data folder
		NodeListResponse formNodeChildren = call(() -> server.client().findNodeChildren(PROJECT_NAME, formUuid));
		Optional<NodeResponse> optDataFolder = formNodeChildren.getData().stream()
				.filter(n -> Strings.CI.equals(n.getDisplayName(), FormService.DATA_FOLDER_NAME)).findFirst();
		assertThat(optDataFolder).as("Form Data Folder").isNotEmpty();

		// post some data (old style)
		Map<String, String> oldDataNodes = new HashMap<>();
		for (String fileName : FILE_UPLOADS) {
			String dataNodeUuid = postFormDataOldStyle(dataFolderUuid, schemaName, "Posted file: %s".formatted(fileName), fileName);
			oldDataNodes.put(dataNodeUuid, fileName);
		}

		// update the form (adding the proper binary field). This should trigger the migration
		JsonNode updateResponse = doRequest(new Request.Builder()
				.url(projectUrl("/forms/" + formUuid))
				.headers(auth(true))
				.post(requestBodyFromFile("migration_single_field_after.json"))
				.build(), true, true, JsonNode.class);

		assertThat(updateResponse).as("Response").isNotNull();
		assertThat(updateResponse.get("uuid").asText()).as("Returned uuid").isEqualTo(formUuid);

		// wait until the form schema has been migrated for the default branch
		waitForMigration(formUuid);

		// post some data (new style)
		Map<String, String> newDataNodes = new HashMap<>();
		for (String fileName : FILE_UPLOADS) {
			String dataNodeUuid = postFormDataNewStyle(formUuid, "Posted file (new): %s".formatted(fileName), fileName);
			newDataNodes.put(dataNodeUuid, fileName);
		}

		assertReport(formUuid, oldDataNodes, newDataNodes);
		assertBinaryURLs(formUuid, oldDataNodes, newDataNodes);
		assertBinariesZip(formUuid, oldDataNodes, newDataNodes);
	}

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

		return createdNode.getUuid();
	}

	protected String postFormDataNewStyle(String formUuid, String inputData, String postedFileName) throws IOException {
		String url = "/form/%s/save?lang=en".formatted(formUuid);

		MultipartBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart(fileUploadKey("file1"), postedFileName, requestBodyFromFile(postedFileName))
				.addFormDataPart("formdata", makeFormdataMultipart(formUuid, null, Map.of("file1", getFileData(postedFileName), "input1", inputData))).build();

		Request postRequest = new Request.Builder().url(projectUrl(url)).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		return JsonUtil.readValue(response.body().string(), FormData.class).getFormDataID();
	}

//	protected void assertFileUploadMigrated(String dataNodeUuid, String fieldName, String fileName) throws IOException {
//		// assert that the data node has the migrated field and it has a UUID set
//		NodeResponse dataNode = call(() -> server.client().findNodeByUuid(PROJECT_NAME, dataNodeUuid));
//		assertThat(dataNode.getFields().getNodeField(fieldName)).as("Migrated field %s".formatted(fieldName))
//				.isNotNull();
//		String binaryItemUuid = dataNode.getFields().getNodeField(fieldName).getUuid();
//		assertThat(binaryItemUuid).as("Binary Item UUID").isNotNull();
//
//		// load the children of the data node, assert that it contains exactly a single node containing the file upload
//		NodeListResponse children = call(() -> server.client().findNodeChildren(PROJECT_NAME, dataNodeUuid));
//		assertThat(children.getData()).as("Data Node Children").areExactly(1, BINARY_ITEM);
//
//		// TODO make further assertions
//	}

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

	protected boolean isSchemaMigrated(String schemaName, String branchUuid) {
		Optional<BranchSchemaInfo> optSchemaInfo = call(
				() -> server.client().getBranchSchemaVersions(PROJECT_NAME, branchUuid)).getSchemas().stream()
				.filter(schema -> Strings.CS.equals(schema.getName(), schemaName)).findFirst();
		assertThat(optSchemaInfo).as("Schema in Branch").isPresent();

		return optSchemaInfo.get().getMigrationStatus() == JobStatus.COMPLETED;
	}

	protected void assertReport(String formUuid, Map<String, String> oldDataNodes, Map<String, String> newDataNodes) throws IOException {
		Set<String> expectedRecords = new HashSet<>();
		// add header
		expectedRecords.add("input1,file1,file1 (filename)");
		// add old records
		for (Map.Entry<String, String> entry : oldDataNodes.entrySet()) {
			String dataNodeUuid = entry.getKey();
			String fileName = entry.getValue();
			expectedRecords.add("Posted file: %s,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file1,%s"
					.formatted(fileName, formUuid, dataNodeUuid, fileName));
		}

		// add new records
		for (Map.Entry<String, String> entry : newDataNodes.entrySet()) {
			String dataNodeUuid = entry.getKey();
			String fileName = entry.getValue();
			expectedRecords.add("Posted file (new): %s,/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file1,%s"
					.formatted(fileName, formUuid, dataNodeUuid, fileName));
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

	protected void assertBinaryURLs(String formUuid, Map<String, String> oldDataNodes, Map<String, String> newDataNodes) throws IOException {
		for (Map.Entry<String, String> entry : oldDataNodes.entrySet()) {
			String dataNodeUuid = entry.getKey();
			String fileName = entry.getValue();
			assertBinaryDownload(formUuid, dataNodeUuid, fileName);
		}
		for (Map.Entry<String, String> entry : newDataNodes.entrySet()) {
			String dataNodeUuid = entry.getKey();
			String fileName = entry.getValue();
			assertBinaryDownload(formUuid, dataNodeUuid, fileName);
		}
	}

	protected void assertBinaryDownload(String formUuid, String dataNodeUuid, String fileName) throws IOException {
		Request downloadRequest = new Request.Builder()
				.url(projectUrl("/forms/%s/data/%s/binary/file1".formatted(formUuid, dataNodeUuid)))
				.headers(auth(true))
				.get()
				.build();
		Response downloadResponse = client().newCall(downloadRequest).execute();

		assertThat(downloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", downloadResponse.code(), downloadResponse.message())).isTrue();
		String download = downloadResponse.body().string();
		assertThat(download).as("Downloaded file").isEqualTo(fromFile(fileName));
	}

	protected void assertBinariesZip(String formUuid, Map<String, String> oldDataNodes, Map<String, String> newDataNodes) {
		// TODO
	}
}
