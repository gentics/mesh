package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.google.common.net.HttpHeaders;
import io.vertx.core.json.JsonObject;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Test the asynchronous export and binary download endpoints.
 */
public class FormDataAsyncReportingTest extends AbstractFormsPluginIntegrationTest {

	@Before
	public void setup() {
		super.setup();
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
		assertThat(setLanguages(PROJECT_NAME, Arrays.asList("en", "de"))).as("Activated languages").containsOnly("en", "de");
	}

	/**
	 * Tests asynchronous export. Checks the status regularly after the request and downloads and verifies the export
	 * when it is available.
	 */
	@Test
	public void testRequestExport() throws IOException, InterruptedException {
		String uuid = createForm("threefields.json");

		// we collect the expected records (including the header) here as Set, because the actual order in
		// we collect the expected records (including the header) here as Set, because the actual order in
		// the CSV is indeterministic, so we will just check that all the records are there
		Set<String> records = new HashSet<>();
		// add "record" for the header
		List<String> headers = Arrays.asList("input1|||First Field", "input2|||Second Field", "input3|||Third Field");

		records.add(StringUtils.join(headers, ","));

		submit(uuid, "input1", "value1", "input2", "value2", "input3", "value3");
		records.add(String.join(",", Arrays.asList("value1", "value2", "value3")));

		Request request = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/export"))
			.headers(auth(true))
			.post(RequestBody.create(null, new byte[0]))
			.build();
		JsonNode response = doRequest(request, true, true, JsonNode.class);
		String downloadUuid = response.get("downloadUuid").asText();

		request = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/download/export/status"))
			.headers(auth(true))
			.get()
			.build();
		response = doRequest(request, true, true, JsonNode.class);

		assertThat(response.get("requestPending").asBoolean())
			.as("Request pending")
			.isTrue();
		assertThat(response.get("downloadReady").asBoolean())
			.as("Download ready")
			.isFalse();

		int numTries = 3;

		while (!response.get("downloadReady").asBoolean() && numTries-- > 0) {
			Thread.sleep(FormDataService.TEST_DELAY_MS);
			response = doRequest(request, true, true, JsonNode.class);
		}

		assertThat(response.get("requestPending").asBoolean())
			.as("Request pending after wait")
			.isFalse();
		assertThat(response.get("downloadReady").asBoolean())
			.as("Download ready after wait")
			.isTrue();
		assertThat(response.get("downloadUuid").asText())
			.as("Download UUID")
			.isEqualTo(downloadUuid);

		// /forms/:formUuid/download/:downloadUuid
		request = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/download/" + downloadUuid))
			.headers(auth(true))
			.get()
			.build();
		Response exportResponse = client().newCall(request).execute();

		assertThat(exportResponse.isSuccessful()).as(String.format("Response success %d (%s)", exportResponse.code(), exportResponse.message())).isTrue();

		Set<String> actualRecords = getExportRecords(exportResponse.body().string());

		assertThat(actualRecords).as("Exported data").containsOnlyElementsOf(records);
	}

	/**
	 * Tests asynchronous binary download. Checks the status regularly after the request and downloads and verifies the
	 * binary download when it is available.
	 */
	@Test
	public void testRequestBinaries() throws IOException, InterruptedException {
		String uuid = createForm("filefield.json");

		List<String> filesToPost = Arrays.asList("list1.json", "list2.json", "list3.json");
		List<String> expectedNames = filesToPost.stream()
			.map(fileName -> {
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
			})
			.collect(Collectors.toList());

		Request request = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/binaries"))
			.headers(auth(true))
			.post(RequestBody.create(null, new byte[0]))
			.build();
		JsonNode response = doRequest(request, true, true, JsonNode.class);
		String downloadUuid = response.get("downloadUuid").asText();

		request = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/download/binaries/status"))
			.headers(auth(true))
			.get()
			.build();
		response = doRequest(request, true, true, JsonNode.class);

		assertThat(response.get("requestPending").asBoolean())
			.as("Request pending")
			.isTrue();
		assertThat(response.get("downloadReady").asBoolean())
			.as("Download ready")
			.isFalse();

		int numTries = 3;

		while (!response.get("downloadReady").asBoolean() && numTries-- > 0) {
			Thread.sleep(FormDataService.TEST_DELAY_MS);
			response = doRequest(request, true, true, JsonNode.class);
		}

		assertThat(response.get("requestPending").asBoolean())
			.as("Request pending after wait")
			.isFalse();
		assertThat(response.get("downloadReady").asBoolean())
			.as("Download ready after wait")
			.isTrue();
		assertThat(response.get("downloadUuid").asText())
			.as("Download UUID")
			.isEqualTo(downloadUuid);

		Request zipDownloadRequest = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/download/" + downloadUuid))
			.headers(auth(true))
			.get()
			.build();
		Response zipDownloadResponse = client().newCall(zipDownloadRequest).execute();

		assertThat(zipDownloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", zipDownloadResponse.code(), zipDownloadResponse.message())).isTrue();

		assertThat(zipDownloadResponse.header(HttpHeaders.CONTENT_DISPOSITION)).as("Content-Disposition").startsWith("attachment; filename=\"binaries_Form with Filefield_").endsWith(".zip\"");
		assertThat(zipDownloadResponse.header(HttpHeaders.CONTENT_TYPE)).as("Content-Type").isEqualTo("application/zip");

		Set<String> foundFiles = new HashSet<>();
		try (ZipInputStream zipIn = new ZipInputStream(zipDownloadResponse.body().byteStream())) {
			ZipEntry entry = null;
			while ((entry = zipIn.getNextEntry()) != null) {
				String name = entry.getName();
				foundFiles.add(name);
				StringWriter fileContents = new StringWriter();
				IOUtils.copy(zipIn, fileContents, "UTF-8");
				assertThat(fileContents.toString()).as(String.format("Contents of file %s", name)).isEqualTo(fromFile(
					name.contains("/")
						? name.substring(name.lastIndexOf("/") + 1)
						: name));
			}
		}
		assertThat(foundFiles).as("Files found in ZIP").containsOnlyElementsOf(expectedNames);
	}
}
