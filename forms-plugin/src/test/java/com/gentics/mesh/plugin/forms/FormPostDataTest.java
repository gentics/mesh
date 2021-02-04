package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.forms.model.response.FormOnlineResponse;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.util.RxUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;

import io.vertx.core.buffer.Buffer;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Test cases for posting data to a form
 */
public class FormPostDataTest extends AbstractFormsPluginIntegrationTest {
	@Before
	public void setup() {
		super.setup();
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
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
		String uuid = createForm("inputfield.json");

		RequestBody requestBody = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("input1", value)
			.build();
		okio.Buffer buffer = new okio.Buffer();
		requestBody.writeTo(buffer);
		System.out.println(buffer.readUtf8());

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref")
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

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
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
		String uuid = createForm("inputfield.json");

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "")
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isRedirect()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.header("Location")).as("Location Header").isEqualTo("ref");
		Optional<Cookie> cookie = Cookie.parseAll(postRequest.url(), response.headers()).stream().filter(c -> FormDataService.COOKIE_NAME.equals(c.name()))
				.findFirst();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
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
		String uuid = createForm("inactive.json");

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "Bla")
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.code()).as("Response code").isEqualTo(205);

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
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
		String uuid = createForm("inactive.json");

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "Bla")
				.addFormDataPart("input2", "Bla")
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.code()).as("Response code").isEqualTo(205);

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
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
		String expectedLink = String.format("http://testhost/api/forms/forms/%s/data/%s/binary/file1", uuids.getLeft(), uuids.getRight());

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
		String uuid = createForm(formFile);

		RequestBody requestBody = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("file1", formFile, requestBodyFromFile(formFile))
			.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);

		MeshBinaryResponse download = call(() -> server.client().downloadBinaryField(PROJECT_NAME, dataNode.getUuid(), dataNode.getLanguage(), "file1"));
		assertThat(download.getFilename()).as("Filename").isEqualTo(formFile);

		Buffer buffer = RxUtil.readEntireData(download.getFlowable()).blockingGet();
		assertThat(buffer.toString()).as("File contents").isEqualTo(fromFile(formFile));

		return Pair.of(uuid, dataNode.getUuid());
	}

	/**
	 * Test updating posted data
	 * @throws IOException
	 */
	@Test
	public void testUpdatePostedData() throws IOException {
		String uuid = createForm("inputfield.json");

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "")
				.build();
		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
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
		postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data/" + formDataUuid)).header("Referer", "ref")
				.post(requestBody).build();
		response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.code()).as("Response code").isEqualTo(205);

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("someValue");
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("1.0");
	}

	/**
	 * Test loading form data
	 * @throws IOException
	 */
	@Test
	public void testLoadFormData() throws IOException {
		String uuid = createForm("inputfield.json");

		for (String value : Arrays.asList("one", "two", "three")) {
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("input1", value)
					.build();
			Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).post(requestBody).build();
			client().newCall(postRequest).execute();
		}

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		for (NodeResponse nodeResponse : dataNodes.getData()) {
			Request loadRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data/" + nodeResponse.getUuid())).get().build();
			Response response = client().newCall(loadRequest).execute();
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

			NodeResponse loadedNode = JsonUtil.readValue(response.body().string(), NodeResponse.class);
			assertThat(loadedNode).as("Loaded Form Data Node").isEqualToComparingOnlyGivenFields(nodeResponse, "uuid");
		}
	}

	/**
	 * Test using the custom element
	 * @throws IOException
	 */
	@Test
	public void testCustomElement() throws IOException {
		String uuid = createForm("custom.json");

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("custom1", "someValue")
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields()).as("Data Node Fields").isNotNull();
		assertThat(dataNode.getFields().getStringField("custom1")).as("Data Node Field").isNotNull();
		assertThat(dataNode.getFields().getStringField("custom1").getString()).as("Data Node Field").isEqualTo("custom someValue");
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("1.0");

		Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + uuid)).header("Accept", "text/html").get().build();
		Response renderResponse = client().newCall(renderRequest).execute();
		assertThat(renderResponse.isSuccessful()).as(String.format("Response success %d (%s)", renderResponse.code(), renderResponse.message())).isTrue();

		assertThat(renderResponse.body().string()).as("Rendered form").isEqualTo(fromFile("custom.html"));
	}

	@Test
	public void testPostDataWithoutPublish() throws Exception {
		String uuid = createForm("inputfield.json");

		RequestBody requestBody = new MultipartBody.Builder()
			.setType(MultipartBody.FORM)
			.addFormDataPart("input1", "someValue")
			.build();

		Request postRequest = new Request.Builder()
			.url(HttpUrl.get(projectUrl("/forms/" + uuid + "/data"))
				.newBuilder()
				.addQueryParameter("publish", "false").build())
			.header("Referer", "ref")
			.post(requestBody)
			.build();
		client().newCall(postRequest).execute();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("someValue");
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("0.1");

		// Empty update to publish
		postRequest = new Request.Builder()
			.url(HttpUrl.get(projectUrl("/forms/" + uuid + "/data/" + dataNode.getUuid())))
			.header("Referer", "ref")
			.post(requestBody)
			.build();
		client().newCall(postRequest).execute();
		dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("someValue");
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("1.0");
	}

	/**
	 * Test posting data to an offline form
	 * @throws IOException
	 */
	@Test
	public void testPostingToOfflineForm() throws IOException {
		String uuid = createForm("create.json", true, true);

		// set form offline
		FormOnlineResponse formOnlineResponse = setFormOffline(uuid, "en", true, true);
		assertThat(formOnlineResponse).as("Response").isNotNull();
		assertThat(formOnlineResponse.isOnline()).as("Form is online").isFalse();

		// post data (expected to not succeed)
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("input1", "bla").build();
		okio.Buffer buffer = new okio.Buffer();
		requestBody.writeTo(buffer);
		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data"))
				.header("Referer", "ref").post(requestBody).build();
		Response postResponse = client().newCall(postRequest).execute();
		assertThat(postResponse.isSuccessful())
				.as(String.format("Response success %d (%s)", postResponse.code(), postResponse.message())).isFalse();
		assertThat(postResponse.code()).as("Response code").isEqualTo(404);
	}
}
