package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.google.common.net.HttpHeaders;
import io.vertx.core.json.JsonObject;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Test cases for getting reporting data
 */
@RunWith(value = Parameterized.class)
public class FormDataReportingTest extends AbstractFormsPluginIntegrationTest {
	@Parameters(name = "{index}: permission {0}, online {1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (Boolean permission : Arrays.asList(true, false)) {
			for (boolean online : Arrays.asList(true, false)) {
				data.add(new Object[] { permission, online });
			}
		}
		return data;
	}

	@Parameter(0)
	public boolean permission;

	@Parameter(1)
	public boolean online;

	@Before
	public void setup() {
		super.setup();
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
		assertThat(setLanguages(PROJECT_NAME, Arrays.asList("en", "de"))).as("Activated languages").containsOnly("en", "de");
	}

	/**
	 * Test exporting data
	 * @throws IOException
	 */
	@Test
	public void testExport() throws IOException {
		doExportTest(false, false);
	}

	/**
	 * Test exporting data where the fields are in reversed order
	 * @throws IOException
	 */
	@Test
	public void testExportReversed() throws IOException {
		doExportTest(true, false);
	}

	/**
	 * Test exporting data including non-completed form data.
	 * @throws IOException
	 */
	@Test
	public void testExportIncludeDraftReversed() throws IOException {
		doExportTest(true, true);
	}

	/**
	 * Test exporting data including non-completed form data.
	 * @throws IOException
	 */
	@Test
	public void testExportIncludeDraft() throws IOException {
		doExportTest(false, true);
	}

	/**
	 * Execute the export test
	 * @param reversed true to execute for form with fields in reversed order
	 * @throws IOException
	 */
	protected void doExportTest(boolean reversed, boolean includeDraft) throws IOException {
		String uuid = createForm(reversed ? "threefields_reversed.json" : "threefields.json");

		// we collect the expected records (including the header) here as Set, because the actual order in
		// the CSV is indeterministic, so we will just check that all the records are there
		Set<String> records = new HashSet<>();
		// add "record" for the header
		List<String> headers = Arrays.asList("input1|||First Field", "input2|||Second Field", "input3|||Third Field");
		if (reversed) {
			Collections.reverse(headers);
		}
		records.add(StringUtils.join(headers, ","));

		String postUrl = projectUrl("/forms/" + uuid + "/data");

		for (String value : Arrays.asList("one", "two", "three")) {
			String value1 = value + " in field 1";
			String value2 = value + " in field 2";
			String value3 = value + " in field 3";
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("input1", value1)
					.addFormDataPart("input2", value2)
					.addFormDataPart("input3", value3)
					.build();
			Request postRequest = new Request.Builder().url(postUrl).post(requestBody).build();
			client().newCall(postRequest).execute();

			// add the line to the set of expected records
			List<String> values = Arrays.asList(value1, value2, value3);
			if (reversed) {
				Collections.reverse(values);
			}
			records.add(StringUtils.join(values, ","));
		}

		// Add a non-complete record.
		List<String> values = new ArrayList<>(3);
		MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder()
			.setType(MultipartBody.FORM);

		for (int ii = 1; ii <= 3; ii++) {
			String value = "unpublished in field" + ii;

			requestBodyBuilder.addFormDataPart("input" + ii, value);
			values.add(value);
		}

		if (includeDraft) {
			if (reversed) {
				Collections.reverse(values);
			}

			records.add(StringUtils.join(values, ","));
		}

		Request postRequest = new Request.Builder()
			.url(postUrl + "?publish=false")
			.post(requestBodyBuilder.build())
			.build();

		client().newCall(postRequest).execute();

		if (!online) {
			setFormOffline(uuid, "en", true, true);
		}

		StringBuilder exportUrl = new StringBuilder(projectUrl("/forms/" + uuid + "/export"));

		if (includeDraft) {
			exportUrl.append("?publishedOnly=false");
		}

		Request exportRequest = new Request.Builder()
				.url(exportUrl.toString())
				.headers(auth(permission))
				.get()
				.build();
		Response exportResponse = client().newCall(exportRequest).execute();

		if (permission) {
			assertThat(exportResponse.isSuccessful()).as(String.format("Response success %d (%s)", exportResponse.code(), exportResponse.message())).isTrue();

			Set<String> actualRecords = getExportRecords(exportResponse.body().string());

			assertThat(actualRecords).as("Exported data").containsOnlyElementsOf(records);
		} else {
			assertForbidden(exportResponse);
		}
	}

	/**
	 * Test exporting data from a form existing only in "de"
	 * @throws IOException
	 */
	@Test
	public void testExportDe() throws IOException {
		String uuid = createForm("threefields_de.json");

		// we collect the expected records (including the header) here as Set, because the actual order in
		// the CSV is indeterministic, so we will just check that all the records are there
		Set<String> records = new HashSet<>();
		// add "record" for the header, input2 does not have a label
		records.add(StringUtils.join(Arrays.asList("input1|||Erstes Feld", "input2", "input3|||Drittes Feld", "input4"), ","));

		for (String value : Arrays.asList("one", "two", "three")) {
			String value1 = value + " in field 1";
			String value2 = value + " in field 2";
			String value3 = value + " in field 3";
			String value4 = value + " in field 4";
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("input1", value1)
					.addFormDataPart("input2", value2)
					.addFormDataPart("input3", value3)
					.addFormDataPart("input4", value4)
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).post(requestBody).build();
			client().newCall(postRequest).execute();

			// add the line to the set of expected records
			records.add(StringUtils.join(Arrays.asList(value1, value2, value3, value4), ","));
		}

		if (!online) {
			setFormOffline(uuid, "de", true, true);
		}

		Request exportRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid + "/export"))
				.headers(auth(permission))
				.get()
				.build();
		Response exportResponse = client().newCall(exportRequest).execute();

		if (permission) {
			assertThat(exportResponse.isSuccessful()).as(String.format("Response success %d (%s)", exportResponse.code(), exportResponse.message())).isTrue();

			Set<String> actualRecords = new HashSet<>();
			try (BufferedReader in = new BufferedReader(new StringReader(exportResponse.body().string()))) {
				String line = null;
				while ((line = in.readLine()) != null) {
					actualRecords.add(line);
				}
			}

			assertThat(actualRecords).as("Exported data").containsOnlyElementsOf(records);
		} else {
			assertForbidden(exportResponse);
		}
	}

	/**
	 * Test exporting data with a custom title row header delimiter
	 * @throws IOException
	 */
	@Test
	public void testExportDeCustomDelimiter() throws IOException {
		String customDelimiter = "CUSTOM_DELIMITER";
		String uuid = createForm("threefields_de.json");

		// we collect the expected records (including the header) here as Set, because the actual order in
		// the CSV is indeterministic, so we will just check that all the records are there
		Set<String> records = new HashSet<>();
		// add "record" for the header, input2 does not have a label
		records.add(StringUtils.join(Arrays.asList("input1" + customDelimiter + "Erstes Feld", "input2", "input3" + customDelimiter + "Drittes Feld", "input4"), ","));

		for (String value : Arrays.asList("one", "two", "three")) {
			String value1 = value + " in field 1";
			String value2 = value + " in field 2";
			String value3 = value + " in field 3";
			String value4 = value + " in field 4";
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("input1", value1)
					.addFormDataPart("input2", value2)
					.addFormDataPart("input3", value3)
					.addFormDataPart("input4", value4)
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).post(requestBody).build();
			client().newCall(postRequest).execute();

			// add the line to the set of expected records
			records.add(StringUtils.join(Arrays.asList(value1, value2, value3, value4), ","));
		}

		if (!online) {
			setFormOffline(uuid, "de", true, true);
		}

		Request exportRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid + "/export?headerDelimiter=" + URLEncoder.encode(customDelimiter, "UTF-8")))
				.headers(auth(permission))
				.get()
				.build();
		Response exportResponse = client().newCall(exportRequest).execute();

		if (permission) {
			assertThat(exportResponse.isSuccessful()).as(String.format("Response success %d (%s)", exportResponse.code(), exportResponse.message())).isTrue();

			Set<String> actualRecords = new HashSet<>();
			try (BufferedReader in = new BufferedReader(new StringReader(exportResponse.body().string()))) {
				String line = null;
				while ((line = in.readLine()) != null) {
					actualRecords.add(line);
				}
			}

			assertThat(actualRecords).as("Exported data").containsOnlyElementsOf(records);
		} else {
			assertForbidden(exportResponse);
		}
	}

	/**
	 * Test "exporting" data for a form, that has no fields
	 * @throws IOException
	 */
	@Test
	public void testExportNoFields() throws IOException {
		String uuid = createForm("button_only_en.json");

		if (!online) {
			setFormOffline(uuid, "en", true, true);
		}

		Request exportRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid + "/export"))
				.headers(auth(permission))
				.get()
				.build();
		Response exportResponse = client().newCall(exportRequest).execute();

		if (permission) {
			assertThat(exportResponse.isSuccessful()).as(String.format("Response success %d (%s)", exportResponse.code(), exportResponse.message())).isTrue();

			Set<String> actualRecords = new HashSet<>();
			try (BufferedReader in = new BufferedReader(new StringReader(exportResponse.body().string()))) {
				String line = null;
				while ((line = in.readLine()) != null) {
					if (!StringUtils.isEmpty(line)) {
						actualRecords.add(line);
					}
				}
			}

			assertThat(actualRecords).as("Exported data").isEmpty();
		} else {
			assertForbidden(exportResponse);
		}
	}

	@Test
	@Ignore("Test requires Elasticsearch")
	public void testListFormData() throws IOException {
		String uuid = createForm("inputfield.json");

		for (String value : Arrays.asList("one", "two", "three")) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("input1", value)
					.build();
			Request postRequest = new Request.Builder()
					.url(projectUrl("/forms/" + uuid + "/data"))
					.post(requestBody)
					.build();
			client().newCall(postRequest).execute();
		}

		if (!online) {
			setFormOffline(uuid, "en", true, true);
		}

		Request loadRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid + "/data"))
				.headers(auth(permission))
				.get()
				.build();
		Response response = client().newCall(loadRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		// TODO more assertions
	}

	/**
	 * Test finding form data
	 * @throws IOException
	 */
	@Test
	@Ignore("Test requires Elasticsearch")
	public void testFindFormData() throws IOException {
		String uuid = createForm("inputfield.json");

		for (String value : Arrays.asList("one", "two", "three")) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("input1", value)
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).post(requestBody).build();
			client().newCall(postRequest).execute();
		}

		if (!online) {
			setFormOffline(uuid, "en", true, true);
		}

		Request findRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data?q=two")).get().build();
		Response response = client().newCall(findRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
	}

	/**
	 * Test export with binary data and default base URL.
	 */
	@Test
	public void testExportBinaryDefaultBaseUrl() throws Exception {
		doTestExportBinary("filefield.json", String.format("/api/v2/%s/plugins/forms/forms", PROJECT_NAME), true);
	}

	/**
	 * Test export with binary data and custom base URL.
	 */
	@Test
	public void testExportBinaryCustomBaseUrl() throws Exception {
		doTestExportBinary("filefield_with_download_url.json", "http://cms.domain/editor/#proxy/CNPortletapp/rest/form/1/data", false);

	}

	private void doTestExportBinary(String filename, String expectedBaseUrl, boolean addFormUuidToBasePath) throws IOException {
		if (!online || !permission) {
			// Permission and online status is covered by other tests. Here we
			// are only interested if the binary fields are exported correctly.

			return;
		}

		String fieldName = "file1";
		String uuid = createForm(filename);
		RequestBody requestBody = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart(fieldName, filename, requestBodyFromFile(filename))
			.build();
		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/jsondata")).post(requestBody).build();
		Response postResponse = client().newCall(postRequest).execute();
		Request exportRequest = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/export"))
			.headers(auth(permission))
			.get()
			.build();
		Response exportResponse = client().newCall(exportRequest).execute();
		String dataUuid = new JsonObject(postResponse.body().string()).getString(FIELD_RS_DATA_UUID);
		String fileUrl = addFormUuidToBasePath
			? String.format("%s/%s/data/%s/binary/%s,%s", expectedBaseUrl, uuid, dataUuid, fieldName, filename)
			: String.format("%s/%s/binary/%s,%s", expectedBaseUrl, dataUuid, fieldName, filename);
		List<String> expected = Arrays.asList(
			String.format("%1$s|||Datei,%1$s|||Datei (filename)", fieldName),
			fileUrl);
		Set<String> actual = getExportRecords(exportResponse.body().string());

		assertThat(actual).as("Exported data").containsOnlyElementsOf(expected);
	}

	/**
	 * Test loading binary data
	 * @throws IOException
	 */
	@Test
	public void testLoadBinaryData() throws IOException {
		String uuid = createForm("filefield.json");

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("file1", "filefield.json", requestBodyFromFile("filefield.json"))
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).post(requestBody).build();
		client().newCall(postRequest).execute();

		if (!online) {
			setFormOffline(uuid, "en", true, true);
		}

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		for (NodeResponse nodeResponse : dataNodes.getData()) {
			Request downloadRequest = new Request.Builder()
					.url(projectUrl("/forms/" + uuid + "/data/" + nodeResponse.getUuid() + "/binary/file1"))
					.headers(auth(permission))
					.get()
					.build();
			Response downloadResponse = client().newCall(downloadRequest).execute();

			if (permission) {
				assertThat(downloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", downloadResponse.code(), downloadResponse.message())).isTrue();
				String download = downloadResponse.body().string();
				assertThat(download).as("Downloaded file").isEqualTo(fromFile("filefield.json"));
				assertThat(downloadResponse.header("Content-Type")).as("Content-Type").isEqualTo("application/json");
				assertThat(downloadResponse.header("Content-Disposition")).as("Content-Disposition").isEqualTo("attachment; filename=\"filefield.json\"");
			} else {
				assertForbidden(downloadResponse);
			}
		}
	}

	/**
	 * Test loading binary data
	 * @throws IOException
	 */
	@Test
	public void testLoadBinaryDataDe() throws IOException {
		String uuid = createForm("filefield_de.json");

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("file1", "filefield.json", requestBodyFromFile("filefield.json"))
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).post(requestBody).build();
		client().newCall(postRequest).execute();

		if (!online) {
			setFormOffline(uuid, "de", true, true);
		}

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		for (NodeResponse nodeResponse : dataNodes.getData()) {
			Request downloadRequest = new Request.Builder()
					.url(projectUrl("/forms/" + uuid + "/data/" + nodeResponse.getUuid() + "/binary/file1"))
					.headers(auth(permission))
					.get()
					.build();
			Response downloadResponse = client().newCall(downloadRequest).execute();

			if (permission) {
				assertThat(downloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", downloadResponse.code(), downloadResponse.message())).isTrue();
				String download = downloadResponse.body().string();
				assertThat(download).as("Downloaded file").isEqualTo(fromFile("filefield.json"));
				assertThat(downloadResponse.header("Content-Type")).as("Content-Type").isEqualTo("application/json");
				assertThat(downloadResponse.header("Content-Disposition")).as("Content-Disposition").isEqualTo("attachment; filename=\"filefield.json\"");
			} else {
				assertForbidden(downloadResponse);
			}
		}
	}

	/**
	 * Test deleting form data
	 * @throws IOException
	 */
	@Test
	public void testDeleteFormData() throws IOException {
		String uuid = createForm("filefield.json");

		for (String value : Arrays.asList("one", "two", "three")) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("input1", value)
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).post(requestBody).build();
			client().newCall(postRequest).execute();
		}

		if (!online) {
			setFormOffline(uuid, "en", true, true);
		}

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		assertThat(dataNodes.getData()).as("List of data nodes").isNotEmpty();
		for (NodeResponse nodeResponse : dataNodes.getData()) {
			NodeListResponse nodes2 = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
			assertThat(nodes2.getData()).as("List before deleting").usingElementComparatorOnFields("uuid").contains(nodeResponse);

			Request deleteRequest = new Request.Builder()
					.url(projectUrl("/forms/" + uuid + "/data/" + nodeResponse.getUuid()))
					.headers(auth(permission))
					.delete()
					.build();
			Response response = client().newCall(deleteRequest).execute();

			if (permission) {
				assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

				nodes2 = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
				assertThat(nodes2.getData()).as("List after deleting").usingElementComparatorOnFields("uuid").doesNotContain(nodeResponse);
			} else {
				assertForbidden(response);
			}
		}
	}

	/**
	 * Test loading binaries as zip
	 * @throws IOException
	 */
	@Test
	public void testLoadBinariesAsZip() throws IOException {
		String uuid = createForm("filefield.json");

		List<String> filesToPost = Arrays.asList("list1.json", "list2.json", "list3.json");
		List<String> expectedNames = filesToPost.stream().map(fileName -> {
			try {
				RequestBody requestBody = new MultipartBody.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart("file1", fileName, requestBodyFromFile(fileName))
						.build();
				Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/jsondata")).header("Referer", "ref").post(requestBody).build();
				Response response = client().newCall(postRequest).execute();
				assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
				return new JsonObject(response.body().string()).getValue("dataUuid") + "/file1/" + fileName;
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}).collect(Collectors.toList());

		if (!online) {
			setFormOffline(uuid, "en", true, true);
		}

		Request zipDownloadRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid + "/binaries"))
				.headers(auth(permission))
				.get()
				.build();
		Response zipDownloadResponse = client().newCall(zipDownloadRequest).execute();

		if (permission) {
			assertThat(zipDownloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", zipDownloadResponse.code(), zipDownloadResponse.message())).isTrue();

			assertThat(zipDownloadResponse.header(HttpHeaders.CONTENT_DISPOSITION)).as("Content-Disposition").startsWith("attachment; filename=forms-files_Form with Filefield_").endsWith(".zip");
			assertThat(zipDownloadResponse.header(HttpHeaders.CONTENT_TYPE)).as("Content-Type").isEqualTo("application/zip");

			Set<String> foundFiles = new HashSet<>();
			try (ZipInputStream zipIn = new ZipInputStream(zipDownloadResponse.body().byteStream())) {
				ZipEntry entry = null;
				while ((entry = zipIn.getNextEntry()) != null) {
					String name = entry.getName();
					foundFiles.add(name);
					StringWriter fileContents = new StringWriter();
					IOUtils.copy(zipIn, fileContents, "UTF-8");
					assertThat(fileContents.toString()).as(String.format("Contents of file %s", name)).isEqualTo(fromFile(name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name));
				}
			}
			assertThat(foundFiles).as("Files found in ZIP").containsOnlyElementsOf(expectedNames);
		} else {
			assertForbidden(zipDownloadResponse);
		}
	}

	/**
	 * Test loading binaries as zip
	 * @throws IOException
	 */
	@Test
	public void testLoadBinariesAsZipDe() throws IOException {
		String uuid = createForm("filefield_de.json");

		List<String> filesToPost = Arrays.asList("list1.json", "list2.json", "list3.json");
		List<String> expectedNames = filesToPost.stream().map(fileName -> {
			try {
				RequestBody requestBody = new MultipartBody.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart("file1", fileName, requestBodyFromFile(fileName))
						.build();
				Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/jsondata")).header("Referer", "ref").post(requestBody).build();
				Response response = client().newCall(postRequest).execute();
				assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
				return new JsonObject(response.body().string()).getValue("dataUuid") + "/file1/" + fileName;
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}).collect(Collectors.toList());

		if (!online) {
			setFormOffline(uuid, "de", true, true);
		}

		Request zipDownloadRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid + "/binaries"))
				.headers(auth(permission))
				.get()
				.build();
		Response zipDownloadResponse = client().newCall(zipDownloadRequest).execute();

		if (permission) {
			assertThat(zipDownloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", zipDownloadResponse.code(), zipDownloadResponse.message())).isTrue();

			assertThat(zipDownloadResponse.header(HttpHeaders.CONTENT_DISPOSITION)).as("Content-Disposition").startsWith("attachment; filename=forms-files_Form with Filefield_").endsWith(".zip");
			assertThat(zipDownloadResponse.header(HttpHeaders.CONTENT_TYPE)).as("Content-Type").isEqualTo("application/zip");

			Set<String> foundFiles = new HashSet<>();
			try (ZipInputStream zipIn = new ZipInputStream(zipDownloadResponse.body().byteStream())) {
				ZipEntry entry = null;
				while ((entry = zipIn.getNextEntry()) != null) {
					String name = entry.getName();
					foundFiles.add(name);
					StringWriter fileContents = new StringWriter();
					IOUtils.copy(zipIn, fileContents, "UTF-8");
					assertThat(fileContents.toString()).as(String.format("Contents of file %s", name)).isEqualTo(fromFile(name.contains("/") ? name.substring(name.lastIndexOf("/") + 1) : name));
				}
			}
			assertThat(foundFiles).as("Files found in ZIP").containsOnlyElementsOf(expectedNames);
		} else {
			assertForbidden(zipDownloadResponse);
		}
	}

	/**
	 * Test handling of duplicate filenames in ZIP download
	 * @throws IOException
	 */
	@Test
	public void testLoadBinariesAsZipDuplicateNames() throws IOException {
		String uuid = createForm("filefield.json");

		List<String> filesToPost = Arrays.asList("list1.json", "list1.json", "list1.json");
		List<String> expectedNames = filesToPost.stream().map(fileName -> {
			try {
				RequestBody requestBody = new MultipartBody.Builder()
						.setType(MultipartBody.FORM)
						.addFormDataPart("file1", fileName, requestBodyFromFile(fileName))
						.build();
				Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/jsondata")).header("Referer", "ref").post(requestBody).build();
				Response response = client().newCall(postRequest).execute();
				assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
				return new JsonObject(response.body().string()).getValue("dataUuid") + "/file1/" + fileName;
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}).collect(Collectors.toList());

		if (!online) {
			setFormOffline(uuid, "en", true, true);
		}

		Request zipDownloadRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid + "/binaries"))
				.headers(auth(permission))
				.get()
				.build();
		Response zipDownloadResponse = client().newCall(zipDownloadRequest).execute();

		if (permission) {
			assertThat(zipDownloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", zipDownloadResponse.code(), zipDownloadResponse.message())).isTrue();

			Set<String> foundFiles = new HashSet<>();
			try (ZipInputStream zipIn = new ZipInputStream(zipDownloadResponse.body().byteStream())) {
				ZipEntry entry = null;
				while ((entry = zipIn.getNextEntry()) != null) {
					String name = entry.getName();
					foundFiles.add(name);
				}
			}
			assertThat(foundFiles).as("Files found in ZIP").containsOnlyElementsOf(expectedNames);
		} else {
			assertForbidden(zipDownloadResponse);
		}
	}

	/**
	 * Test loading binaries as zip, when there is no data
	 * @throws IOException
	 */
	@Test
	public void testLoadBinariesAsZipNoData() throws IOException {
		String uuid = createForm("filefield.json");

		if (!online) {
			setFormOffline(uuid, "en", true, true);
		}

		Request zipDownloadRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid + "/binaries"))
				.headers(auth(permission))
				.get()
				.build();
		Response zipDownloadResponse = client().newCall(zipDownloadRequest).execute();
		if (permission) {
			assertThat(zipDownloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", zipDownloadResponse.code(), zipDownloadResponse.message())).isTrue();
		} else {
			assertForbidden(zipDownloadResponse);
		}
	}

	/**
	 * Test loading binaries as zip, when form has no file fields
	 * @throws IOException
	 */
	@Test
	public void testLoadBinariesAsZipNoFileElements() throws IOException {
		String uuid = createForm("inputfield.json");

		if (!online) {
			setFormOffline(uuid, "en", true, true);
		}

		Request zipDownloadRequest = new Request.Builder()
				.url(projectUrl("/forms/" + uuid + "/binaries"))
				.headers(auth(permission))
				.get()
				.build();
		Response zipDownloadResponse = client().newCall(zipDownloadRequest).execute();
		if (permission) {
			assertThat(zipDownloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", zipDownloadResponse.code(), zipDownloadResponse.message())).isTrue();
		} else {
			assertForbidden(zipDownloadResponse);
		}
	}

	private Set<String> getExportRecords(String export) throws IOException {
		Set<String> records = new HashSet<>();

		try (BufferedReader in = new BufferedReader(new StringReader(export))) {
			String line;

			while ((line = in.readLine()) != null) {
				records.add(line);
			}
		}

		return records;
	}
}
