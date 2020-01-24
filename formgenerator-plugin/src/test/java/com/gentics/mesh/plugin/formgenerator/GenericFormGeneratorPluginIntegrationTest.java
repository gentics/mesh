package com.gentics.mesh.plugin.formgenerator;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.plugin.PluginConfigUtil;
import com.gentics.mesh.plugin.common.service.MeshPluginService;
import com.gentics.mesh.plugin.formgenerator.config.FormGeneratorConfig;
import com.gentics.mesh.plugin.formgenerator.handler.LoadFilesZipHandler;
import com.gentics.mesh.plugin.formgenerator.service.FormDataService;
import com.gentics.mesh.plugin.formgenerator.service.SchemaService;
import com.gentics.mesh.plugin.formgenerator.util.RxUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.google.common.net.HttpHeaders;
import com.jayway.jsonpath.JsonPath;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GenericFormGeneratorPluginIntegrationTest extends AbstractFormGeneratorPluginIntegrationTest {

	/**
	 * Test whether schemas have been created and assigned to the project
	 */
	@Test
	public void testInitialization() {
		// assert schemas globally
		SchemaListResponse schemas = call(() -> server.client().findSchemas(new PagingParametersImpl().setPerPage(Long.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas")
				.contains(SchemaService.PLUGIN_SCHEMA_NAME, SchemaService.SCHEMA_NAME, SchemaService.TEMPLATE_SCHEMA_NAME);

		// assert schemas for project
		schemas = call(() -> server.client().findSchemas(PROJECT_NAME, new PagingParametersImpl().setPerPage(Long.MAX_VALUE)));
		assertThat(schemas.getData().stream().map(SchemaResponse::getName).collect(Collectors.toList())).as("List of schemas for project")
				.contains(SchemaService.PLUGIN_SCHEMA_NAME, SchemaService.SCHEMA_NAME, SchemaService.TEMPLATE_SCHEMA_NAME);
	}

	/**
	 * Test creating a new form
	 * @throws IOException
	 */
	@Test
	public void testCreateForm() throws IOException {
		Response response = createForm("create.json");

		NodeResponse nodeResponse = JsonUtil.readValue(response.body().string(), NodeResponse.class);
		assertThat(nodeResponse.getProject().getName()).as("NodeResponse Project").isEqualTo(PROJECT_NAME);
		assertThat(nodeResponse.getFields().getStringField("name").getString()).as("NodeResponse name").isEqualTo("Created Form");
		JsonObject loadedJson = JsonUtil.readValue(nodeResponse.getFields().getStringField("json").getString(), JsonObject.class);
		JsonObject expectedJson = JsonUtil.readValue(fromFile("create.json"), JsonObject.class);
		assertThat(loadedJson).as("NodeResponse json").isEqualTo(expectedJson);

		Optional<NodeResponse> optionalNode = loadNode(nodeResponse.getUuid());
		assertThat(optionalNode).as("Form Node").isPresent();
		assertThat(optionalNode.get().getParentNode().getSchema().getName()).as("Parent schema").isEqualTo(SchemaService.PLUGIN_SCHEMA_NAME);
		assertThat(loadSchema(SchemaService.getSchemaName(nodeResponse.getUuid()))).as("Form Schema").isPresent();
	}

	/**
	 * Test listing forms
	 * @throws IOException
	 */
	@Test
	public void testListForms() throws IOException {
		createForm("list1.json");
		createForm("list2.json");
		createForm("list3.json");

		Request listRequest = new Request.Builder()
				.url(projectUrl("/forms"))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.get()
				.build();
		Response response = client().newCall(listRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		String responseBody = response.body().string();
		int totalCount = JsonPath.read(responseBody, "$.totalCount");
		assertThat(totalCount).as("Total count").isEqualTo(3);
		List<String> names = JsonPath.read(responseBody, "$.forms[*].name");
		assertThat(names).as("Form names").containsOnly("Form List 1", "Form List 2", "Form List 3");
	}

	/**
	 * Test loading a specific form
	 * @throws IOException
	 */
	@Test
	public void testLoadForm() throws IOException {
		Response createResponse = createForm("create.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		Request loadRequest = new Request.Builder()
				.url(projectUrl("/forms/" + createdNode.getUuid()))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.get()
				.build();
		Response response = client().newCall(loadRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		NodeResponse loadedNode = JsonUtil.readValue(response.body().string(), NodeResponse.class);
		assertThat(loadedNode).as("Loaded Form Node").isEqualToComparingOnlyGivenFields(createdNode, "uuid", "displayName");
	}

	/**
	 * Test updating a form
	 * @throws IOException
	 */
	@Test
	public void testUpdateForm() throws IOException {
		NodeResponse updatedNode = updateForm("update.json", true);

		assertThat(updatedNode.getDisplayName())
			.as("Name of updated form")
			.isEqualTo("Updated Form");
	}

	/**
	 * Test deleting a form
	 * @throws IOException
	 */
	@Test
	public void testDeleteForm() throws IOException {
		Response createResponse = createForm("create.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		Request deleteRequest = new Request.Builder()
				.url(projectUrl("/forms/" + createdNode.getUuid()))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.delete()
				.build();
		Response response = client().newCall(deleteRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertThat(loadNode(createdNode.getUuid())).as("Form Node").isEmpty();
		assertThat(loadSchema(SchemaService.getSchemaName(createdNode.getUuid()))).as("Form Schema").isEmpty();
	}

	/**
	 * Test loading the schema for a form
	 * @throws IOException
	 */
	@Test
	public void testLoadSchema() throws IOException {
		Response createResponse = createForm("withPageId.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		Request loadSchemaRequest = new Request.Builder()
				.url(projectUrl("/forms/" + createdNode.getUuid() + "/schema"))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.get()
				.build();
		Response response = client().newCall(loadSchemaRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		String responseBody = response.body().string();
		String schemaName = JsonPath.read(responseBody, "$.name");
		assertThat(schemaName).as("Schema name").isEqualTo(SchemaService.getSchemaName(createdNode.getUuid()));
		int pageId = JsonPath.read(responseBody, "$.pageId");
		assertThat(pageId).as("Page ID").isEqualTo(4711);
	}

	/**
	 * Test rendering a form
	 * @throws IOException
	 */
	@Test
	public void testRender() throws IOException {
		Response createResponse = createForm("inputfield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid())).header("Accept", "text/html").get().build();
		Response response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("inputfield.html"));
	}

	/**
	 * Test posting data to a form
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPostMultilineData() throws IOException {
		runCaseForValue("\nsome\nValue\n");
	}

	/**
	 * Test posting data to a form
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPostData() throws IOException {
		runCaseForValue("someValue");
	}

	private void runCaseForValue(String value) throws GenericRestException, IOException {
		Response createResponse = createForm("inputfield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		RequestBody requestBody = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("input1", value)
			.build();
		okio.Buffer buffer = new okio.Buffer();
		requestBody.writeTo(buffer);
		System.out.println(buffer.readUtf8());

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).header("Referer", "ref")
			.post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.code()).as("Response code").isEqualTo(205);
		Optional<Cookie> cookie = Cookie.parseAll(postRequest.url(), response.headers()).stream()
			.filter(c -> FormDataService.COOKIE_NAME.equals(c.name()))
			.findFirst();

		assertThat(cookie).as("Cookie").isPresent();
		assertThat(cookie.get()).as("Cookie").matches(c -> "".equals(c.value()), "Value is empty").matches(
			c -> c.expiresAt() < System.currentTimeMillis(),
			"is expired");

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo(value);
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("1.0");
	}

	/**
	 * Test posting incomplete data to a form
	 * @throws IOException
	 */
	@Test
	public void testPostDataIncomplete() throws IOException {
		Response createResponse = createForm("inputfield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "")
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isRedirect()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.header("Location")).as("Location Header").isEqualTo("ref");
		Optional<Cookie> cookie = Cookie.parseAll(postRequest.url(), response.headers()).stream().filter(c -> FormDataService.COOKIE_NAME.equals(c.name()))
				.findFirst();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("");
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("0.1");

		assertThat(cookie).as("Cookie").isPresent();
		assertThat(cookie.get()).as("Cookie").matches(c -> dataNode.getUuid().equals(c.value()), "Value is not empty");
	}

	/**
	 * Test posting data to a form that has a mandatory, but inactive element
	 * @throws IOException
	 */
	@Test
	public void testPostDataIncompleteInactive() throws IOException {
		Response createResponse = createForm("inactive.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "Bla")
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.code()).as("Response code").isEqualTo(205);

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field input1").isEqualTo("Bla");
		assertThat(dataNode.getFields().getStringField("input2")).as("Data Node Field input2").isNull();
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("1.0");
	}

	/**
	 * Test posting data to a form with an inactive element (data for the element must be ignored)
	 * @throws IOException
	 */
	@Test
	public void testPostDataInactive() throws IOException {
		Response createResponse = createForm("inactive.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "Bla")
				.addFormDataPart("input2", "Bla")
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.code()).as("Response code").isEqualTo(205);

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field input1").isEqualTo("Bla");
		assertThat(dataNode.getFields().getStringField("input2")).as("Data Node Field input2").isNull();
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("1.0");
	}

	/**
	 * Test posting a file
	 * @throws IOException
	 */
	@Test
	public void testPostFile() throws IOException {
		doPostFile("filefield.json");
	}

	@Test
	public void testPostFileWithMail() throws IOException, MessagingException {
		String formDefinition = "filefield_with_mail.json";
		Pair<String, String> uuids = doPostFile(formDefinition);

		boolean mailSent = greenMail.waitForIncomingEmail(5000, 1);

		assertThat(mailSent)
			.as("Email sent in time")
			.isTrue();

		MimeMessage[] messages = greenMail.getReceivedMessages();

		assertThat(messages.length)
			.as("Received messages")
			.isEqualTo(1);

		MimeMultipart content = (MimeMultipart) messages[0].getContent();

		// Mail must contain the message from testmail.hbs and the file as an attachment.
		assertThat(content.getCount())
			.as("Number of parts")
			.isEqualTo(2);

		BodyPart part = content.getBodyPart(0);
		String expectedLink = String.format("http://testhost/api/fg/forms/%s/data/%s/binary/file1", uuids.getLeft(), uuids.getRight());

		assertThat(part.getContent().toString().trim())
			.as("Message")
			.isEqualTo(expectedLink);

		part = content.getBodyPart(1);

		InputStream fileContents = (InputStream) part.getContent();

		// doPostFile() will post the JSON file containing the form definition.
		assertThat(IOUtils.toString(fileContents, "utf-8"))
			.as("Attached file")
			.isEqualTo(fromFile(formDefinition));
	}

	private Pair<String, String> doPostFile(String formFile) throws IOException {
		Response createResponse = createForm(formFile);
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		RequestBody requestBody = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("file1", formFile, requestBodyFromFile(formFile))
			.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);

		MeshBinaryResponse download = call(() -> server.client().downloadBinaryField(PROJECT_NAME, dataNode.getUuid(), dataNode.getLanguage(), "file1"));
		assertThat(download.getFilename()).as("Filename").isEqualTo(formFile);

		Buffer buffer = RxUtil.readEntireData(download.getFlowable()).blockingGet();
		assertThat(buffer.toString()).as("File contents").isEqualTo(fromFile(formFile));

		return Pair.of(createdNode.getUuid(), dataNode.getUuid());
	}

	/**
	 * Test updating posted data
	 * @throws IOException
	 */
	@Test
	public void testUpdatePostedData() throws IOException {
		Response createResponse = createForm("inputfield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "")
				.build();
		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isRedirect()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		Optional<Cookie> cookie = Cookie.parseAll(postRequest.url(), response.headers()).stream().filter(c -> FormDataService.COOKIE_NAME.equals(c.name()))
				.findFirst();
		assertThat(cookie).as("Cookie").isPresent();
		String formDataUuid = cookie.get().value();

		requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "someValue")
				.build();
		postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data/" + formDataUuid)).header("Referer", "ref")
				.post(requestBody).build();
		response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.code()).as("Response code").isEqualTo(205);

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("someValue");
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("1.0");
	}

	/**
	 * Test exporting data
	 * @throws IOException
	 */
	@Test
	@Ignore("Order of entries in CSV export is indeterministic")
	public void testExport() throws IOException {
		Response createResponse = createForm("threefields.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		List<List<String>> records = new ArrayList<>();

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
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).post(requestBody).build();
			client().newCall(postRequest).execute();

			records.add(0, Arrays.asList(value1, value2, value3));
		}

		Request exportRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/export")).get().build();
		Response exportResponse = client().newCall(exportRequest).execute();
		assertThat(exportResponse.isSuccessful()).as(String.format("Response success %d (%s)", exportResponse.code(), exportResponse.message())).isTrue();

		StringWriter writer = new StringWriter();
		try (CSVPrinter csv = new CSVPrinter(writer, CSVFormat.Predefined.Default.getFormat().withHeader("First Field", "Second Field", "Third Field"))) {
			csv.printRecords(records);
		}
		assertThat(exportResponse.body().string()).as("Exported data").isEqualTo(writer.toString());
	}

	/**
	 * Test loading form data
	 * @throws IOException
	 */
	@Test
	public void testLoadFormData() throws IOException {
		Response createResponse = createForm("inputfield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		for (String value : Arrays.asList("one", "two", "three")) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("input1", value)
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).post(requestBody).build();
			client().newCall(postRequest).execute();
		}

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
		for (NodeResponse nodeResponse : dataNodes.getData()) {
			Request loadRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data/" + nodeResponse.getUuid())).get().build();
			Response response = client().newCall(loadRequest).execute();
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

			NodeResponse loadedNode = JsonUtil.readValue(response.body().string(), NodeResponse.class);
			assertThat(loadedNode).as("Loaded Form Data Node").isEqualToComparingOnlyGivenFields(nodeResponse, "uuid");
		}
	}

	/**
	 * Test finding form data
	 * @throws IOException
	 */
	@Test
	@Ignore("Test requires Elasticsearch")
	public void testFindFormData() throws IOException {
		Response createResponse = createForm("inputfield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		for (String value : Arrays.asList("one", "two", "three")) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("input1", value)
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).post(requestBody).build();
			client().newCall(postRequest).execute();
		}

		Request findRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data?q=two")).get().build();
		Response response = client().newCall(findRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
	}

	/**
	 * Test loading binary data
	 * @throws IOException
	 */
	@Test
	public void testLoadBinaryData() throws IOException {
		Response createResponse = createForm("filefield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("file1", "filefield.json", requestBodyFromFile("filefield.json"))
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).post(requestBody).build();
		client().newCall(postRequest).execute();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
		for (NodeResponse nodeResponse : dataNodes.getData()) {
			Request downloadRequest = new Request.Builder()
					.url(projectUrl("/forms/" + createdNode.getUuid() + "/data/" + nodeResponse.getUuid() + "/binary/file1"))
					.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
					.get()
					.build();
			Response downloadResponse = client().newCall(downloadRequest).execute();
			assertThat(downloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", downloadResponse.code(), downloadResponse.message())).isTrue();
			String download = downloadResponse.body().string();
			assertThat(download).as("Downloaded file").isEqualTo(fromFile("filefield.json"));
			assertThat(downloadResponse.header("Content-Type")).as("Content-Type").isEqualTo("application/json");
			assertThat(downloadResponse.header("Content-Disposition")).as("Content-Disposition").isEqualTo("attachment; filename=\"filefield.json\"");
		}
	}

	/**
	 * Test deleting form data
	 * @throws IOException
	 */
	@Test
	public void testDeleteFormData() throws IOException {
		Response createResponse = createForm("filefield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		for (String value : Arrays.asList("one", "two", "three")) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("input1", value)
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).post(requestBody).build();
			client().newCall(postRequest).execute();
		}

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
		assertThat(dataNodes.getData()).as("List of data nodes").isNotEmpty();
		for (NodeResponse nodeResponse : dataNodes.getData()) {
			NodeListResponse nodes2 = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
			assertThat(nodes2.getData()).as("List before deleting").usingElementComparatorOnFields("uuid").contains(nodeResponse);

			Request deleteRequest = new Request.Builder()
					.url(projectUrl("/forms/" + createdNode.getUuid() + "/data/" + nodeResponse.getUuid()))
					.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
					.delete()
					.build();
			Response response = client().newCall(deleteRequest).execute();
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

			nodes2 = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
			assertThat(nodes2.getData()).as("List after deleting").usingElementComparatorOnFields("uuid").doesNotContain(nodeResponse);
		}
	}

	/**
	 * Test creating a template
	 * @throws IOException
	 */
	@Test
	public void testCreateTemplate() throws IOException {
		Response response = createTemplate("createtemplate.json");

		String responseBody = response.body().string();
		String id = JsonPath.read(responseBody, "$.id");
		String name = JsonPath.read(responseBody, "$.name");
		String json = JsonPath.read(responseBody, "$.json");

		assertThat(id).as("Template ID").isNotEmpty();
		assertThat(name).as("Template name").isEqualTo("Created Template");
		assertThat(json).as("Template Json").isNotEmpty();

		assertThat(loadNode(id)).as("Node").isPresent();
	}

	/**
	 * Test listing templates
	 * @throws IOException
	 */
	@Test
	public void testListTemplates() throws IOException {
		createTemplate("listtemplate1.json");
		createTemplate("listtemplate2.json");
		createTemplate("listtemplate3.json");

		Request listRequest = new Request.Builder()
				.url(projectUrl("/templates"))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.get()
				.build();
		Response response = client().newCall(listRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		String responseBody = response.body().string();
		List<String> names = JsonPath.read(responseBody, "$.templates.global[*].name");
		assertThat(names).as("Template names").containsOnly("Template List 1", "Template List 2", "Template List 3");
	}

	/**
	 * Test loading a template
	 * @throws IOException
	 */
	@Test
	public void testLoadTemplate() throws IOException {
		Response createResponse = createTemplate("createtemplate.json");
		String createResponseBody = createResponse.body().string();
		String id = JsonPath.read(createResponseBody, "$.id");

		Request loadRequest = new Request.Builder()
				.url(projectUrl("/templates/" + id))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.get()
				.build();
		Response loadResponse = client().newCall(loadRequest).execute();
		assertThat(loadResponse.isSuccessful()).as(String.format("Response success %d (%s)", loadResponse.code(), loadResponse.message())).isTrue();

		String loadResponseBody = loadResponse.body().string();

		assertThat(loadResponseBody).as("Load Response").isEqualTo(createResponseBody);
	}

	/**
	 * Test deleting a template
	 * @throws IOException
	 */
	@Test
	public void testDeleteTemplate() throws IOException {
		Response createResponse = createTemplate("createtemplate.json");
		String createResponseBody = createResponse.body().string();
		String id = JsonPath.read(createResponseBody, "$.id");

		Request deleteRequest = new Request.Builder()
				.url(projectUrl("/templates/" + id))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.delete()
				.build();
		Response deleteResponse = client().newCall(deleteRequest).execute();
		assertThat(deleteResponse.isSuccessful()).as(String.format("Response success %d (%s)", deleteResponse.code(), deleteResponse.message())).isTrue();

		assertThat(loadNode(id)).as("Node").isEmpty();
	}

	/**
	 * Test whether initialization moves existing form nodes into the plugin root node
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testFormNodeMigration() throws IOException, InterruptedException {
		int numNodes = 100;
		String[] names = new String[numNodes];
		Arrays.fill(names, "Form to Move");
		String graphQl = fromFile("form_nodes_in_root.graphql");
		String pluginRootgraphQl = fromFile("plugin_root_nodes.graphql");

		// create form nodes and form template nodes in the project root
		Completable createFormNodes = server.client().findProjectByName(PROJECT_NAME).toSingle().flatMapCompletable(project -> {
			return Observable.fromArray(names).flatMapCompletable(name -> {
				NodeCreateRequest create = new NodeCreateRequest()
						.setSchemaName(SchemaService.SCHEMA_NAME)
						.setLanguage("en")
						.setParentNode(project.getRootNode());
				create.getFields().put("name", new StringFieldImpl().setString(name));
				return server.client().createNode(PROJECT_NAME, create).toSingle().flatMapCompletable(response -> server.client().publishNode(PROJECT_NAME, response.getUuid()).toCompletable());
			}).andThen(Observable.fromArray(names).flatMapCompletable(name -> {
				NodeCreateRequest create = new NodeCreateRequest()
						.setSchemaName(SchemaService.TEMPLATE_SCHEMA_NAME)
						.setLanguage("en")
						.setParentNode(project.getRootNode());
				create.getFields().put("name", new StringFieldImpl().setString(name));
				return server.client().createNode(PROJECT_NAME, create).toSingle().flatMapCompletable(response -> server.client().publishNode(PROJECT_NAME, response.getUuid()).toCompletable());
			}));
		});

		await(createFormNodes, 1, TimeUnit.MINUTES);

		int countBeforeMigration = JsonPath.read(call(() -> server.client().graphqlQuery(PROJECT_NAME, graphQl)).toJson(), "$.data.rootNode.children.totalCount");
		assertThat(countBeforeMigration).as("Form Nodes in root before migration").isEqualTo(numNodes * 2);

		int pluginRootNodesBeforeMigration = JsonPath.read(call(() -> server.client().graphqlQuery(PROJECT_NAME, pluginRootgraphQl)).toJson(), "$.data.schema.nodes.totalCount");
		assertThat(pluginRootNodesBeforeMigration).as("Plugin root nodes before migration").isEqualTo(1);

		// run the initialization
		Map<String, String> rootNodeUuids = new HashMap<>();
		rootNodeUuids.put(PROJECT_NAME, TestUtil.findRootNode(server.client(), PROJECT_NAME).blockingGet().getUuid());
		SchemaService schemaService = new SchemaService(server.client(), rootNodeUuids);
		FormGeneratorConfig pluginConfig = PluginConfigUtil.getYAMLMapper().readValue(FormGeneratorPlugin.class.getResourceAsStream("config.yml"), FormGeneratorConfig.class);
		MeshPluginService meshPluginService = new MeshPluginService(server.getMesh().getRxVertx(), server.client(), new File("."));
		SchemaInitializer schemaInitializer = new SchemaInitializer(server.client(), pluginConfig, meshPluginService, schemaService, new HashMap<>());
		await(schemaInitializer.moveFormsAndTemplates(PROJECT_NAME), 1, TimeUnit.MINUTES);

		int countAfterMigration = JsonPath.read(call(() -> server.client().graphqlQuery(PROJECT_NAME, graphQl)).toJson(), "$.data.rootNode.children.totalCount");
		assertThat(countAfterMigration).as("Form Nodes in root after migration").isEqualTo(0);

		int pluginRootNodesAfterMigration = JsonPath.read(call(() -> server.client().graphqlQuery(PROJECT_NAME, pluginRootgraphQl)).toJson(), "$.data.schema.nodes.totalCount");
		assertThat(pluginRootNodesAfterMigration).as("Plugin root nodes after migration").isEqualTo(1);
	}

	/**
	 * Test that the form schema is updated, when the form is changed
	 * @throws IOException
	 */
	@Test
	public void testUpdateFormSchema() throws IOException {
		Response createResponse = createForm("inputfield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		Optional<SchemaResponse> optionalSchema = loadSchema(SchemaService.getSchemaName(createdNode.getUuid()));
		assertThat(optionalSchema).as("Schema").isPresent();
		assertThat(optionalSchema.get().getFields()).as("Schema Fields").usingFieldByFieldElementComparator()
				.containsExactly(new StringFieldSchemaImpl().setName("input1").setLabel("Text"), new StringFieldSchemaImpl().setName("errors").setLabel("Errors"));

		Request updateRequest = new Request.Builder()
				.url(projectUrl("/forms/" + createdNode.getUuid()))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.post(requestBodyFromFile("threefields.json"))
				.build();
		client().newCall(updateRequest).execute();

		optionalSchema = loadSchema(SchemaService.getSchemaName(createdNode.getUuid()));
		assertThat(optionalSchema).as("Schema").isPresent();
		assertThat(optionalSchema.get().getFields()).as("Schema Fields").usingFieldByFieldElementComparator()
				.containsExactly(
						new StringFieldSchemaImpl().setName("input1").setLabel("First Field"),
						new StringFieldSchemaImpl().setName("input2").setLabel("Second Field"),
						new StringFieldSchemaImpl().setName("input3").setLabel("Third Field"),
						new StringFieldSchemaImpl().setName("errors").setLabel("Errors"));
	}

	@Test
	public void testFilenameUnique() {
		Map<String, String> filenameReplacements = new HashMap<>();
		filenameReplacements.put("list.json", "list (1).json");
		filenameReplacements.put("list (1).json", "list (2).json");
		filenameReplacements.put("list", "list (1)");
		filenameReplacements.put("list (1)", "list (2)");

		for (Map.Entry<String, String> entry : filenameReplacements.entrySet()) {
			String orig = entry.getKey();
			String replacement = LoadFilesZipHandler.getNextFilename(orig);
			String expected = entry.getValue();
			assertThat(replacement).as(String.format("Replacement for %s", orig)).isEqualTo(expected);
		}
	}

	/**
	 * Test loading binaries as zip
	 * @throws IOException
	 */
	@Test
	public void testLoadBinariesAsZip() throws IOException {
		Response createResponse = createForm("filefield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		List<String> filesToPost = Arrays.asList("list1.json", "list2.json", "list3.json");

		for (String fileName : filesToPost) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("file1", fileName, requestBodyFromFile(fileName))
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).header("Referer", "ref").post(requestBody).build();
			Response response = client().newCall(postRequest).execute();
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		}

		Request zipDownloadRequest = new Request.Builder()
				.url(projectUrl("/forms/" + createdNode.getUuid() + "/binaries"))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.get()
				.build();
		Response zipDownloadResponse = client().newCall(zipDownloadRequest).execute();
		assertThat(zipDownloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", zipDownloadResponse.code(), zipDownloadResponse.message())).isTrue();

		assertThat(zipDownloadResponse.header(HttpHeaders.CONTENT_DISPOSITION)).as("Content-Disposition").startsWith("attachment; filename=fg-files_Form with Filefield_").endsWith(".zip");
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
	}

	/**
	 * Test handling of duplicate filenames in ZIP download
	 * @throws IOException
	 */
	@Test
	public void testLoadBinariesAsZipDuplicateNames() throws IOException {
		Response createResponse = createForm("filefield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		List<String> filesToPost = Arrays.asList("list1.json", "list1.json", "list1.json");
		List<String> expectedNames = Arrays.asList("list1.json", "list1 (1).json", "list1 (2).json");

		for (String fileName : filesToPost) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("file1", fileName, requestBodyFromFile(fileName))
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + createdNode.getUuid() + "/data")).header("Referer", "ref").post(requestBody).build();
			Response response = client().newCall(postRequest).execute();
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		}

		Request zipDownloadRequest = new Request.Builder()
				.url(projectUrl("/forms/" + createdNode.getUuid() + "/binaries"))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.get()
				.build();
		Response zipDownloadResponse = client().newCall(zipDownloadRequest).execute();
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
	}

	/**
	 * Test loading binaries as zip, when there is no data
	 * @throws IOException
	 */
	@Test
	public void testLoadBinariesAsZipNoData() throws IOException {
		Response createResponse = createForm("filefield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		Request zipDownloadRequest = new Request.Builder()
				.url(projectUrl("/forms/" + createdNode.getUuid() + "/binaries"))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.get()
				.build();
		Response zipDownloadResponse = client().newCall(zipDownloadRequest).execute();
		assertThat(zipDownloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", zipDownloadResponse.code(), zipDownloadResponse.message())).isTrue();
	}

	/**
	 * Test loading binaries as zip, when form has no file fields
	 * @throws IOException
	 */
	@Test
	public void testLoadBinariesAsZipNoFileElements() throws IOException {
		Response createResponse = createForm("inputfield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		Request zipDownloadRequest = new Request.Builder()
				.url(projectUrl("/forms/" + createdNode.getUuid() + "/binaries"))
				.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
				.get()
				.build();
		Response zipDownloadResponse = client().newCall(zipDownloadRequest).execute();
		assertThat(zipDownloadResponse.isSuccessful()).as(String.format("Response success %d (%s)", zipDownloadResponse.code(), zipDownloadResponse.message())).isTrue();
	}

	/**
	 * Test default field with custom properties
	 * @throws IOException
	 */
	@Test
	public void testFieldWithProperties() throws IOException {
		Response createResponse = createForm("inputfieldWithProperties.json");
		NodeResponse nodeResponse = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		JsonObject loadedJson = JsonUtil.readValue(nodeResponse.getFields().getStringField("json").getString(), JsonObject.class);
		JsonObject expectedJson = JsonUtil.readValue(fromFile("inputfieldWithProperties.json"), JsonObject.class);
		assertThat(loadedJson).as("NodeResponse json").isEqualTo(expectedJson);

		Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + nodeResponse.getUuid())).header("Accept", "text/html").get().build();
		Response response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("inputfieldWithProperties.html"));
	}

	/**
	 * Test using the custom element
	 * @throws IOException
	 */
	@Test
	public void testCustomElement() throws IOException {
		Response createResponse = createForm("custom.json");
		NodeResponse nodeResponse = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("custom1", "someValue")
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + nodeResponse.getUuid() + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, nodeResponse.getUuid()));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields()).as("Data Node Fields").isNotNull();
		assertThat(dataNode.getFields().getStringField("custom1")).as("Data Node Field").isNotNull();
		assertThat(dataNode.getFields().getStringField("custom1").getString()).as("Data Node Field").isEqualTo("custom someValue");
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("1.0");

		Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + nodeResponse.getUuid())).header("Accept", "text/html").get().build();
		Response renderResponse = client().newCall(renderRequest).execute();
		assertThat(renderResponse.isSuccessful()).as(String.format("Response success %d (%s)", renderResponse.code(), renderResponse.message())).isTrue();

		assertThat(renderResponse.body().string()).as("Rendered form").isEqualTo(fromFile("custom.html"));
	}

	@Test
	public void testPostDataWithoutPublish() throws Exception {
		Response createResponse = createForm("inputfield.json");
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);

		RequestBody requestBody = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("input1", "someValue")
			.build();

		Request postRequest = new Request.Builder()
			.url(HttpUrl.get(projectUrl("/forms/" + createdNode.getUuid() + "/data"))
				.newBuilder()
				.addQueryParameter("publish", "false").build())
			.header("Referer", "ref")
			.post(requestBody)
			.build();
		client().newCall(postRequest).execute();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("someValue");
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("0.1");

		// Empty update to publish
		postRequest = new Request.Builder()
			.url(HttpUrl.get(projectUrl("/forms/" + createdNode.getUuid() + "/data/" + dataNode.getUuid())))
			.header("Referer", "ref")
			.post(requestBody)
			.build();
		client().newCall(postRequest).execute();
		dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, createdNode.getUuid()));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("someValue");
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("1.0");

	}
}
