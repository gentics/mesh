package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.google.common.net.HttpHeaders;

import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
		String uuid = createForm("threefields.json");

		// we collect the expected records (including the header) here as Set, because the actual order in
		// the CSV is indeterministic, so we will just check that all the records are there
		Set<String> records = new HashSet<>();
		// add "record" for the header
		records.add(StringUtils.join(Arrays.asList("First Field", "Second Field", "Third Field"), ","));

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
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).post(requestBody).build();
			client().newCall(postRequest).execute();

			// add the line to the set of expected records
			records.add(StringUtils.join(Arrays.asList(value1, value2, value3), ","));
		}

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
					actualRecords.add(line);
				}
			}

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
		// add "record" for the header
		records.add(StringUtils.join(Arrays.asList("Erstes Feld", "Zweites Feld", "Drittes Feld"), ","));

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
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).post(requestBody).build();
			client().newCall(postRequest).execute();

			// add the line to the set of expected records
			records.add(StringUtils.join(Arrays.asList(value1, value2, value3), ","));
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

		for (String fileName : filesToPost) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("file1", fileName, requestBodyFromFile(fileName))
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
			Response response = client().newCall(postRequest).execute();
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		}

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
					assertThat(fileContents.toString()).as(String.format("Contents of file %s", name)).isEqualTo(fromFile(name));
				}
			}

			assertThat(foundFiles).as("Files found in ZIP").containsOnlyElementsOf(filesToPost);
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

		for (String fileName : filesToPost) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("file1", fileName, requestBodyFromFile(fileName))
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
			Response response = client().newCall(postRequest).execute();
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		}

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
					assertThat(fileContents.toString()).as(String.format("Contents of file %s", name)).isEqualTo(fromFile(name));
				}
			}

			assertThat(foundFiles).as("Files found in ZIP").containsOnlyElementsOf(filesToPost);
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
		List<String> expectedNames = Arrays.asList("list1.json", "list1 (1).json", "list1 (2).json");

		for (String fileName : filesToPost) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("file1", fileName, requestBodyFromFile(fileName))
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
			Response response = client().newCall(postRequest).execute();
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		}

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
}
