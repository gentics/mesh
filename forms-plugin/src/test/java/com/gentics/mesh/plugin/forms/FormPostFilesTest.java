package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.forms.model.schema.FormData;
import com.gentics.mesh.plugin.forms.model.schema.FormDataProperty;

import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Tests for POSTing binary uploads
 */
public class FormPostFilesTest extends AbstractFormPostDataTest {
	public final static String SINGLE_FILE_FORM = "filefield.json";

	public final static String MULTI_FILE_FORM = "multifilefield.json";

	public final static String DATEFIELD_FORM = "datefield.json";

	public final static String INPUTFIELD_FORM = "inputfield.json";

	public final static String FIELD_NAME = "file1";

	public final static Pattern MESH_UUID_PATTERM = Pattern.compile("^mesh: (\\w{8}\\w{4}\\w{4}\\w{4}\\w{12})$");

	@Test
	public void testSaveSingleFile() throws IOException {
		String formUuid = createForm(SINGLE_FILE_FORM);

		FormData responseData = postSingleFile(formUuid, null, MULTI_FILE_FORM);
		assertSingleFile(formUuid, responseData, MULTI_FILE_FORM);
	}

	@Test
	public void testUpdateSingleFile() throws IOException {
		String formUuid = createForm(SINGLE_FILE_FORM);

		// post a file
		FormData responseData = postSingleFile(formUuid, null, SINGLE_FILE_FORM);

		// post another file
		responseData = postSingleFile(formUuid, responseData.getFormDataID(), MULTI_FILE_FORM);
		assertSingleFile(formUuid, responseData, MULTI_FILE_FORM);
	}

	@Test
	public void testRemoveSingleFile() throws IOException {
		String formUuid = createForm(SINGLE_FILE_FORM);

		// post a file
		FormData responseData = postSingleFile(formUuid, null, SINGLE_FILE_FORM);

		// save and remove the file
		responseData.getProperties().put(FIELD_NAME, new FormDataProperty().setValue(Map.of()));
		responseData = save(formUuid, responseData.getFormDataID(), responseData);
		assertNoFile(formUuid, responseData);
	}

	@Test
	public void testSaveMultipleFiles() throws IOException {
		String formUuid = createForm(MULTI_FILE_FORM);

		// post multiple files
		FormData responseData = postMultipleFiles(formUuid, null, Pair.of(SINGLE_FILE_FORM, null), Pair.of(MULTI_FILE_FORM, null));
		assertMultipleFiles(formUuid, responseData, SINGLE_FILE_FORM, MULTI_FILE_FORM);
	}

	@Test
	public void testAddMultipleFiles() throws IOException {
		String formUuid = createForm(MULTI_FILE_FORM);

		// post multiple files
		FormData responseData = postMultipleFiles(formUuid, null, Pair.of(SINGLE_FILE_FORM, null), Pair.of(MULTI_FILE_FORM, null));

		List<Object> values = responseData.getProperties().get(FIELD_NAME).getValues();

		// save and add a file
		responseData = postMultipleFiles(formUuid, responseData.getFormDataID(), Pair.of(SINGLE_FILE_FORM, values.get(0)),
				Pair.of(MULTI_FILE_FORM, values.get(1)), Pair.of(DATEFIELD_FORM, null));

		assertMultipleFiles(formUuid, responseData, SINGLE_FILE_FORM, MULTI_FILE_FORM, DATEFIELD_FORM);
	}

	@Test
	public void testRemoveMultipleFiles() throws IOException {
		String formUuid = createForm(MULTI_FILE_FORM);

		// post multiple files
		FormData responseData = postMultipleFiles(formUuid, null, Pair.of(SINGLE_FILE_FORM, null), Pair.of(MULTI_FILE_FORM, null), Pair.of(DATEFIELD_FORM, null));

		List<Object> values = responseData.getProperties().get(FIELD_NAME).getValues();

		// save and remove a file
		responseData = postMultipleFiles(formUuid, responseData.getFormDataID(), Pair.of(MULTI_FILE_FORM, values.get(1)), Pair.of(DATEFIELD_FORM, values.get(2)));
		assertMultipleFiles(formUuid, responseData, MULTI_FILE_FORM, DATEFIELD_FORM);
	}

	@Test
	public void testAddAndRemoveMultipleFiles() throws IOException {
		String formUuid = createForm(MULTI_FILE_FORM);

		// post multiple files
		FormData responseData = postMultipleFiles(formUuid, null, Pair.of(SINGLE_FILE_FORM, null), Pair.of(MULTI_FILE_FORM, null), Pair.of(DATEFIELD_FORM, null));

		List<Object> values = responseData.getProperties().get(FIELD_NAME).getValues();

		// save, add and remove a file
		responseData = postMultipleFiles(formUuid, responseData.getFormDataID(), Pair.of(SINGLE_FILE_FORM, values.get(0)), Pair.of(DATEFIELD_FORM, values.get(2)), Pair.of(INPUTFIELD_FORM, null));
		assertMultipleFiles(formUuid, responseData, SINGLE_FILE_FORM, DATEFIELD_FORM, INPUTFIELD_FORM);
	}

	@Test
	public void testValidateEmpty() throws IOException {
		String formUuid = createForm(MULTI_FILE_FORM);

		FormData toValidate = new FormData();
		toValidate.setFormID(formUuid);
		toValidate.getProperties().put(FIELD_NAME, new FormDataProperty().setValues(List.of()));

		FormData responseData = validate(formUuid, null, toValidate);
		assertThat(responseData.getProperties()).as("Response properties").containsKey(FIELD_NAME);
		assertThat(responseData.getProperties().get(FIELD_NAME)).as("Response property " + FIELD_NAME)
				.hasFieldOrPropertyWithValue("values", List.of());
	}

	protected String extractMeshUuid(String value) {
		Matcher m = MESH_UUID_PATTERM.matcher(value);
		if (m.matches()) {
			return m.group(1);
		} else {
			fail("'%s' does not match expected pattern".formatted(value));
			return null;
		}
	}

	protected FormData postSingleFile(String formUuid, String formDataUuid, String postedFile) throws IOException {
		MultipartBody.Builder requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart(fileUploadKey(FIELD_NAME), postedFile, requestBodyFromFile(postedFile))
				.addFormDataPart("formdata", makeFormdataMultipart(formUuid, null, Map.of(FIELD_NAME, getFileData(postedFile))));

		String url = "/form/%s/save?lang=en".formatted(formUuid);
		if (formDataUuid != null) {
			url = url + "&formDataUuid=%s".formatted(formDataUuid);
		}

		Request postRequest = new Request.Builder().url(projectUrl(url)).header("Referer", "ref").post(requestBody.build()).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		return JsonUtil.readValue(response.body().string(), FormData.class);
	}

	@SafeVarargs
	protected final FormData postMultipleFiles(String formUuid, String formDataUuid, Pair<String, Object>...postedFiles) throws IOException {
		MultipartBody.Builder requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM);

		List<Object> formDataFiles = new ArrayList<>();
		for (int i = 0; i < postedFiles.length; i++) {
			String fileName = postedFiles[i].getKey();
			Object formData = postedFiles[i].getValue();

			if (formData == null) {
				formDataFiles.add(getFileData(fileName));
				requestBody.addFormDataPart(fileUploadKey("file1", i), fileName, requestBodyFromFile(fileName));
			} else {
				formDataFiles.add(formData);
			}
		}
		requestBody.addFormDataPart("formdata", makeFormdataMultipart(formUuid, formDataUuid, Map.of(FIELD_NAME, formDataFiles)));

		String url = "/form/%s/save?lang=en".formatted(formUuid);
		if (formDataUuid != null) {
			url = url + "&formDataUuid=%s".formatted(formDataUuid);
		}

		Request postRequest = new Request.Builder().url(projectUrl(url)).header("Referer", "ref").post(requestBody.build()).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		return JsonUtil.readValue(response.body().string(), FormData.class);
	}

	protected FormData validate(String formUuid, String formDataUuid, FormData validatedFormData) throws IOException {
		MultipartBody.Builder requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("formdata", JsonUtil.toJson(validatedFormData));

		String url = "/form/%s/validate?lang=en".formatted(formUuid);
		if (formDataUuid != null) {
			url = url + "&formDataUuid=%s".formatted(formDataUuid);
		}

		Request postRequest = new Request.Builder().url(projectUrl(url)).header("Referer", "ref").post(requestBody.build()).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		return JsonUtil.readValue(response.body().string(), FormData.class);
	}

	protected FormData save(String formUuid, String formDataUuid, FormData savedFormData) throws IOException {
		MultipartBody.Builder requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("formdata", JsonUtil.toJson(savedFormData));

		String url = "/form/%s/save?lang=en".formatted(formUuid);
		if (formDataUuid != null) {
			url = url + "&formDataUuid=%s".formatted(formDataUuid);
		}

		Request postRequest = new Request.Builder().url(projectUrl(url)).header("Referer", "ref").post(requestBody.build()).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		return JsonUtil.readValue(response.body().string(), FormData.class);
	}

	protected void assertSingleFile(String formUuid, FormData responseData, String expectedFile) {
		assertThat(responseData.getProperties().get(FIELD_NAME)).as(FIELD_NAME).isNotNull();
		assertThat(responseData.getProperties().get(FIELD_NAME).getValue()).as(FIELD_NAME + " value").isInstanceOf(Map.class);
		if (responseData.getProperties().get(FIELD_NAME).getValue() instanceof Map fieldValue) {
			assertThat(fieldValue).as(FIELD_NAME).containsEntry("fileName", expectedFile);

			if (fieldValue.get("value") instanceof String valueString) {
				String binaryUuid = extractMeshUuid(valueString);

				NodeResponse dataNode = assertSingleDataNode(formUuid, n -> {
					assertThat(n.getFields().getNodeField(FIELD_NAME)).as("Data Node Field").isNotNull()
							.hasFieldOrPropertyWithValue("uuid", binaryUuid);
				});

				assertBinaryNodes(dataNode.getUuid(), Map.of(binaryUuid, expectedFile));
			} else {
				fail("value should be a string");
			}
		}
	}

	protected void assertMultipleFiles(String formUuid, FormData responseData, String...expectedFiles) {
		assertThat(responseData.getProperties().get(FIELD_NAME)).as(FIELD_NAME).isNotNull();
		assertThat(responseData.getProperties().get(FIELD_NAME).getValues()).as(FIELD_NAME + " values").isNotNull();

		Map<String, String> expectedMap = new HashMap<>();

		for (Object value : responseData.getProperties().get(FIELD_NAME).getValues()) {
			assertThat(value).as("Value").isInstanceOf(Map.class);
			if (value instanceof Map fieldValue) {
				if (fieldValue.get("value") instanceof String valueString) {
					String binaryUuid = extractMeshUuid(valueString);

					if (fieldValue.get("fileName") instanceof String fileNameString) {
						expectedMap.put(binaryUuid, fileNameString);
					}
				}
			}
		}

		assertThat(expectedMap.values()).as("Expected files").containsOnly(expectedFiles);

		NodeResponse dataNode = assertSingleDataNode(formUuid, n -> {
			assertThat(n.getFields().getNodeFieldList(FIELD_NAME)).as("Data Node Field").isNotNull();
			assertThat(n.getFields().getNodeFieldList(FIELD_NAME).getItems().stream().map(NodeFieldListItem::getUuid))
					.as("Data Node Field").hasSameElementsAs(expectedMap.keySet());
		});

		assertBinaryNodes(dataNode.getUuid(), expectedMap);
	}

	protected void assertNoFile(String formUuid, FormData responseData) {
		assertThat(responseData.getProperties().get(FIELD_NAME)).as(FIELD_NAME).isNotNull();
		assertThat(responseData.getProperties().get(FIELD_NAME).getValue()).as(FIELD_NAME + " value").isInstanceOf(Map.class);
		if (responseData.getProperties().get(FIELD_NAME).getValue() instanceof Map fieldValue) {
			assertThat(fieldValue).as(FIELD_NAME).isEmpty();

			NodeResponse dataNode = assertSingleDataNode(formUuid, n -> {
				assertThat(n.getFields().getNodeField(FIELD_NAME)).as("Data Node Field").isNull();
			});

			assertBinaryNodes(dataNode.getUuid(), Map.of());
		}
	}

	protected void assertBinaryNodes(String dataNodeUuid, Map<String, String> binaryMap) {
		NodeListResponse dataNodeChildren = call(() -> server.client().findNodeChildren(PROJECT_NAME, dataNodeUuid));
		if (binaryMap.isEmpty()) {
			assertThat(dataNodeChildren.getData()).as("Binary Nodes").isEmpty();
		} else {
			assertThat(dataNodeChildren.getData().stream().map(n -> n.getSchema().getName())).as("Binary Nodes Schemas")
					.containsOnly(SchemaInitializer.MULTIBINARY_ITEM_SCHEMA_NAME);

			Map<String, String> childrenMap = dataNodeChildren.getData().stream().collect(
					Collectors.toMap(NodeResponse::getUuid, n -> n.getFields().getBinaryField("binary").getFileName()));

			assertThat(childrenMap).as("Binary Nodes").containsExactlyInAnyOrderEntriesOf(binaryMap);
		}
	}
}
