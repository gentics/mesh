package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.gentics.mesh.plugin.forms.model.PostDataResponse;
import com.gentics.mesh.plugin.forms.test.ResponseWithCookies;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.forms.config.ReCaptchaOptions;
import com.gentics.mesh.plugin.forms.model.PostDataResponse;
import com.gentics.mesh.plugin.forms.model.response.FormOnlineResponse;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.service.MailService;
import com.gentics.mesh.plugin.forms.test.ResponseWithCookies;
import com.gentics.mesh.plugin.forms.util.RxUtil;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.logging.LogDelegate;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test cases for posting data to a form
 */
@RunWith(value = Parameterized.class)
public class FormPostDataTest extends AbstractFormsPluginIntegrationTest {

	// Set to a higher value for debugging (value is in ms).
	private static final int MAIL_TIMEOUT = 5_000;

	@Parameters(name = "{index}: language {0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> data = new ArrayList<>();
		for (String language : Arrays.asList("en", "it")) {
			data.add(new Object[] { language });
		}
		return data;
	}

	@Parameter(0)
	public String language;

	/**
	 * Name of the file containing the definition of the form with a single input field (in the tested language)
	 */
	protected String inputFieldForm;

	/**
	 * Name of the file containing the definition of a form with an inactive element (in the tested language)
	 */
	protected String inactiveForm;

	/**
	 * web server mock which will act as verification server
	 */
	protected MockWebServer webServer;

	@Before
	public void setup() {
		super.setup();

		// start the mock recaptcha verification server
		webServer = new MockWebServer();
		try {
			webServer.start();
		} catch (IOException e) {
			fail("Failed to start mock web-server");
		}

		// modify the plugin configuration: set the recaptcha verification URL
		ReCaptchaOptions reCaptchaOptions = FormsTestPlugin.instance.configuration.getReCaptcha();
		reCaptchaOptions.setVerificationUrl(String.format("http://%s:%s/", webServer.getHostName(), webServer.getPort()));

		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
		setLanguages(PROJECT_NAME, Arrays.asList("en", "it"));
		inputFieldForm = "inputfield_languages_" + language + ".json";
		inactiveForm = "inactive_" + language + ".json";
	}

	@After
	public void tearDown() throws IOException {
		if (webServer != null) {
			webServer.shutdown();
		}
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
		String uuid = createForm(inputFieldForm);

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
			.filter(c -> FormDataService.COOKIE_NAME_DATA.equals(c.name()))
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
		String uuid = createForm(inputFieldForm);

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "")
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isRedirect()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.header("Location")).as("Location Header").isEqualTo("ref");
		Optional<Cookie> cookie = Cookie.parseAll(postRequest.url(), response.headers()).stream().filter(c -> FormDataService.COOKIE_NAME_DATA.equals(c.name()))
				.findFirst();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("");
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("0.1");

		Request getRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data/" + dataNode.getUuid())).get().build();

		response = client().newCall(getRequest).execute();

		assertThat(response.code())
			.as("Response code")
			.isEqualTo(HttpResponseStatus.OK.code());

		assertThat(cookie).as("Cookie").isPresent();
		assertThat(cookie.get()).as("Cookie").matches(c -> dataNode.getUuid().equals(c.value()), "Value is not empty");
	}

	/**
	 * Test posting data to a form that has a mandatory, but inactive element
	 * @throws IOException
	 */
	@Test
	public void testPostDataIncompleteInactive() throws IOException {
		String uuid = createForm(inactiveForm);

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
		String uuid = createForm(inactiveForm);

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
		Pair<String, String> data = doPostFile("filefield.json");
		Request request = new Request.Builder()
			.url(projectUrl("/forms/" + data.getLeft() + "/data/" + data.getRight() + "/binary/file1"))
			.get()
			.build();
		Response response = client().newCall(request).execute();

		assertThat(response.code())
			.as("Anonymous response code")
			.isEqualTo(HttpResponseStatus.FORBIDDEN.code());


		request = new Request.Builder()
			.url(projectUrl("/forms/" + data.getLeft() + "/data/" + data.getRight() + "/binary/file1"))
			.headers(auth(true))
			.get()
			.build();
		response = client().newCall(request).execute();

		assertThat(response.code())
			.as("Authenticated response code")
			.isEqualTo(HttpResponseStatus.OK.code());
	}

	/**
	 * Test usage of direct mailsource in Form and UserMailElement.
	 */
	@Test
	public void testMailTemplate() throws IOException, MessagingException {
		String uuid = createForm("mailsource.json");
		ResponseWithCookies rs = submit(uuid, true, false, false, "usermail1", "user@test.com");

		assertThat(greenMail.waitForIncomingEmail(MAIL_TIMEOUT, 2))
			.as("Mail sent in time")
			.isTrue();

		MimeMessage[] messages = greenMail.getReceivedMessages();

		assertThat(messages.length)
			.as("Received messages")
			.isEqualTo(2);

		for (MimeMessage msg : messages) {
			String subject = msg.getSubject();
			String content = msg.getContent().toString().replaceAll("\\r", "");

			System.out.println(content);

			assertThat(content.trim())
				.as(String.format("Mail content for %s", subject))
				.isEqualTo(String.format("Custom %s source, value: user@test.com", subject));
		}
	}

	/**
	 * Test the date format helper.
	 */
	@Test
	public void testDateFormat() throws IOException, MessagingException {
		String uuid = createForm("datefield.json");
		ResponseWithCookies rs = submit(uuid, true, false, false, "date1", "23.05.1970");
		assertThat(greenMail.waitForIncomingEmail(MAIL_TIMEOUT, 1))
			.as("Mail sent in time")
			.isTrue();

		MimeMessage[] messages = greenMail.getReceivedMessages();

		assertThat(messages.length)
			.as("Received messages")
			.isEqualTo(1);

		String content = messages[0].getContent().toString().replaceAll("\\r", "");

		System.out.println(content);

		assertThat(content)
			.as("Mail content")
			.isEqualTo(fromFile("datefield.html"));
	}

	/**
	 * Verify that the admin email is taken from the {@link com.gentics.mesh.plugin.forms.model.AdminMailElement},
	 * and not from the configured mail in the form.
	 */
	@Test
	public void testAdminMailElement() throws IOException, MessagingException {
		String uuid = createForm("admin_mail_element.json");
		ResponseWithCookies rs = submit(uuid, true, false, false, "input1", "test input", "adminmail1", "Admin 1");

		assertThat(greenMail.waitForIncomingEmail(MAIL_TIMEOUT, 1))
			.as("Mail sent in time")
			.isTrue();

		MimeMessage[] messages = greenMail.getReceivedMessages();

		assertThat(messages.length)
			.as("Received messages")
			.isEqualTo(1);

		List<String> recipients = Stream.of(messages[0].getRecipients(Message.RecipientType.TO))
			.map(Address::toString)
			.collect(Collectors.toList());

		assertThat(recipients)
			.as("Mail recipients")
			.containsExactlyInAnyOrder("admin1@green.mail");

		String content = messages[0].getContent().toString().replaceAll("\\r", "");

		assertThat(content)
			.as("Mail content")
			.isEqualTo(fromFile("admin_mail_element_mail.html"));
	}

	/**
	 * Verify that the admin emails are taken from the {@link com.gentics.mesh.plugin.forms.model.AdminMailElement},
	 * and not from the configured mail in the form.
	 */
	@Test
	public void testAdminMailElementMultiple() throws IOException, MessagingException {
		String uuid = createForm("admin_mail_element_multiselect.json");
		ResponseWithCookies rs = submit(uuid, true, false, false, "input1", "test input", "adminmail1", "Admin 1", "adminmail1", "Admin 3");

		assertThat(greenMail.waitForIncomingEmail(MAIL_TIMEOUT, 1))
			.as("Mail sent in time")
			.isTrue();

		MimeMessage[] messages = greenMail.getReceivedMessages();

		assertThat(messages.length)
			.as("Received messages")
			.isEqualTo(2);

		List<String> recipients = Stream.of(messages[0].getRecipients(Message.RecipientType.TO))
			.map(Address::toString)
			.collect(Collectors.toList());

		assertThat(recipients)
			.as("Mail recipients")
			.containsExactlyInAnyOrder("admin1@green.mail", "admin3@green.mail");

		String content = messages[0].getContent().toString().replaceAll("\\r", "");

		assertThat(content)
			.as("Mail content")
			.isEqualTo(fromFile("admin_mail_element_mail_multiple.html"));
	}

	/**
	 * Test rendering of selection elements for mails.
	 */
	@Test
	public void testSelectionWithMail() throws IOException, MessagingException {
		String uuid = createForm("selection_with_mail.json");
		ResponseWithCookies rs = submit(uuid, true, false, false,
			"checkbox1", "X",
			"radiobuttons1", "r_2",
			"singleselect1", "s_1",
			"multiselect1", "m_1", "multiselect1", "m_2", "multiselect1", "m_3");

		assertThat(greenMail.waitForIncomingEmail(MAIL_TIMEOUT, 1))
			.as("Mail sent in time")
			.isTrue();

		MimeMessage[] messages = greenMail.getReceivedMessages();

		assertThat(messages.length)
			.as("Received messages")
			.isEqualTo(1);

		String content = messages[0].getContent().toString().replaceAll("\\r", "");

		System.out.println(content);

		assertThat(content)
			.as("Mail content")
			.isEqualTo(fromFile("selection_with_mail.html"));
	}

	/**
	 * Test rendering for checkbox elements for mails.
	 */
	@Test
	public void testEmptySelectionWithMail() throws IOException, MessagingException {
		String uuid = createForm("selection_with_mail.json");
		ResponseWithCookies rs = submit(uuid, true, false, false,
			// The body must contain at least one field
			"checkbox1", "X");

		assertThat(greenMail.waitForIncomingEmail(MAIL_TIMEOUT, 1))
			.as("Mail sent in time")
			.isTrue();

		MimeMessage[] messages = greenMail.getReceivedMessages();

		assertThat(messages.length)
			.as("Received messages")
			.isEqualTo(1);

		String content = messages[0].getContent().toString().replaceAll("\\r", "");

		System.out.println(content);

		assertThat(content)
			.as("Mail content")
			.isEqualTo(fromFile("empty_selection_with_mail.html"));
	}

	@Test
	public void testCheckboxWithMail() throws IOException, MessagingException {
		String uuid = createForm("selection_with_mail.json");
		ResponseWithCookies rs = submit(uuid, true, false, false,
			"radiobuttons1", "r_2",
			"singleselect1", "s_1",
			"multiselect1", "m_1", "multiselect1", "m_2", "multiselect1", "m_3");

		assertThat(greenMail.waitForIncomingEmail(MAIL_TIMEOUT, 1))
			.as("Mail sent in time")
			.isTrue();

		MimeMessage[] messages = greenMail.getReceivedMessages();

		assertThat(messages.length)
			.as("Received messages")
			.isEqualTo(1);

		String content = messages[0].getContent().toString().replaceAll("\\r", "");

		System.out.println(content);

		assertThat(content)
			.as("Mail content")
			.isEqualTo(fromFile("selection_with_mail_unchecked_checkbox.html"));
	}

	/**
	 * Test rendering posted files for mails.
	 */
	@Test
	public void testPostFileWithEmailDefaultBaseUrl() throws MessagingException, IOException {
		doTestPostFileWithMail(
			"filefield_with_mail.json",
			"/api/v2/testproject/plugins/forms/forms/%s/data/%s/binary/file1", true);
	}

	@Test
	public void testPostFileWithEmailCustomBaseUrl() throws MessagingException, IOException {
		doTestPostFileWithMail(
			"filefield_with_mail_custom_download_url.json",
			"http://cms.domain/editor/#proxy/CNPortletapp/rest/form/1/data/%s/binary/file1", false);
	}

	private void doTestPostFileWithMail(String formDefinition, String expectedLinkFormat, boolean addFormUuid) throws IOException, MessagingException {
		Pair<String, String> uuids = doPostFile(formDefinition);

		boolean mailSent = greenMail.waitForIncomingEmail(MAIL_TIMEOUT, 1);

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

		String expectedLink = addFormUuid
			? String.format(expectedLinkFormat, uuids.getLeft(), uuids.getRight())
			: String.format(expectedLinkFormat, uuids.getRight());

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

	/**
	 * Test that trying to send an admin mail without admin mail template configured will log an error message.
	 * @throws IOException
	 */
	@Test
	public void testPostAdminMailNoTemplate() throws IOException {
		Logger vertxLogger = LoggerFactory.getLogger(MailService.class);
		LogDelegate delegate = vertxLogger.getDelegate();
		ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)delegate.unwrap();

		@SuppressWarnings("unchecked")
		Appender<ILoggingEvent> appender = mock(Appender.class);
		try {
			logger.addAppender(appender);
			// create the form
			String uuid = createForm("adminmail_notemplate.json", true, true);

			// post data
			RequestBody requestBody = new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart("input1", "someValue")
					.build();
			Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody);
			assertOk(pair.getLeft());

			ArgumentCaptor<ILoggingEvent> argument = ArgumentCaptor.forClass(ILoggingEvent.class);
			verify(appender).doAppend(argument.capture());
			assertThat(argument.getValue()).as("Logging Event").hasFieldOrPropertyWithValue("level", Level.ERROR)
					.hasFieldOrPropertyWithValue("formattedMessage",
							"Could not send admin mail for form Form without admin mail template, because no template configuration was found (template must be either configured in config.yml as \"adminMailTemplate\" or set in the form as \"mailtemp\").");
		} finally {
			logger.detachAppender(appender);
		}
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
		String uuid = createForm(inputFieldForm);

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "")
				.build();
		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isRedirect()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		Optional<Cookie> cookie = Cookie.parseAll(postRequest.url(), response.headers()).stream().filter(c -> FormDataService.COOKIE_NAME_DATA.equals(c.name()))
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
		String uuid = createForm(inputFieldForm);

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
			assertThat(response.code())
				.as("Published form data must not be readable")
				.isEqualTo(HttpResponseStatus.NOT_FOUND.code());
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

	/**
	 * Test using the custom element
	 * @throws IOException
	 */
	@Test
	public void testCustomConfigElement() throws IOException {
		String uuid = createForm("customconfig.json");

		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("customconfig1", "someValue")
				.build();

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data")).header("Referer", "ref").post(requestBody).build();
		Response response = client().newCall(postRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);
		assertThat(dataNode.getFields()).as("Data Node Fields").isNotNull();
		assertThat(dataNode.getFields().getStringField("customconfig1")).as("Data Node Field").isNotNull();
		assertThat(dataNode.getFields().getStringField("customconfig1").getString()).as("Data Node Field").isEqualTo("custom someValue");
		assertThat(dataNode.getVersion()).as("Data Node Version").isEqualTo("1.0");

		Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + uuid)).header("Accept", "text/html").get().build();
		Response renderResponse = client().newCall(renderRequest).execute();
		assertThat(renderResponse.isSuccessful()).as(String.format("Response success %d (%s)", renderResponse.code(), renderResponse.message())).isTrue();

		assertThat(renderResponse.body().string()).as("Rendered form").isEqualTo(fromFile("customconfig.html"));
	}

	@Test
	public void testPostDataWithoutPublish() throws Exception {
		String uuid = createForm(inputFieldForm);

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

	/**
	 * Test posting to a form containing a recaptcha field (which is not filled)
	 * @throws IOException
	 */
	@Test
	public void testRecaptchaNotFilled() throws IOException {
		// create the form
		String uuid = createForm("recaptcha.json", true, true);

		// post only the input field
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "someValue")
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody);

		// we expect no calls to the recaptcha verification server
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(0);

		// we expect to be redirected back to the form
		assertRedirect(pair.getLeft());
		// data node must not be published
		NodeResponse dataNode = assertSingleDataNode(uuid, n -> {
			assertThat(n.getVersion()).as("Data Node Version").isEqualTo("0.1");
			assertThat(n.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("someValue");
		});
		// form-data cookie must exist
		assertThat(pair.getRight()).as("Cookie").isPresent();
		assertThat(pair.getRight().get()).as("Cookie").matches(c -> dataNode.getUuid().equals(c.value()), "Value is not empty");
	}

	/**
	 * Test posting to a form containing a recaptcha field (which is incorrectly filled)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testRecaptchaIncorrectlyFilled() throws IOException, InterruptedException {
		// create the form
		String uuid = createForm("recaptcha.json", true, true);

		// let the recaptcha verification server answer with success: false
		webServer.enqueue(new MockResponse().setBody("{\"success\": false}"));

		// post the input field and an incorrect recaptcha response
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "someValue")
				.addFormDataPart("g-recaptcha-response", "incorrect")
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody);

		// recaptcha verification server must have answered one request
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(1);
		RecordedRequest request = webServer.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getBody().toString()).as("Request body").isEqualTo("[text=secret=your-secret-key&response=incorrect]");

		// we expect to be redirected back to the form
		assertRedirect(pair.getLeft());
		// data node must not be published
		NodeResponse dataNode = assertSingleDataNode(uuid, n -> {
			assertThat(n.getVersion()).as("Data Node Version").isEqualTo("0.1");
			assertThat(n.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("someValue");
		});
		// form-data cookie must exist
		assertThat(pair.getRight()).as("Cookie").isPresent();
		assertThat(pair.getRight().get()).as("Cookie").matches(c -> dataNode.getUuid().equals(c.value()), "Value is not empty");
	}

	/**
	 * Test posting to a form containing a recaptcha field (which is correctly filled)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testRecaptchaCorrectlyFilled() throws IOException, InterruptedException {
		// create the form
		String uuid = createForm("recaptcha.json", true, true);

		// let the recaptcha verification server answer with success: true
		webServer.enqueue(new MockResponse().setBody("{\"success\": true}"));

		// post the input field and a correct recaptcha response
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("input1", "someValue")
				.addFormDataPart("g-recaptcha-response", "correct")
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody);

		// recaptcha verification server must have answered one request
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(1);
		RecordedRequest request = webServer.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getBody().toString()).as("Request body").isEqualTo("[text=secret=your-secret-key&response=correct]");

		// answer to form post must have been OK
		assertOk(pair.getLeft());
	}

	/**
	 * Test posting to a multipage form containing a recaptcha field (which is not filled)
	 * @throws IOException
	 */
	@Test
	public void testMultipageRecaptchaNotFilled() throws IOException {
		// create the form
		String uuid = createForm("multipage_recaptcha.json", true, true);

		// post input field from page 1
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("page1", "1")
				.addFormDataPart("input1", "someValue")
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody);

		// we expect no calls to the recaptcha verification server
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(0);

		// we expect to be redirected back to the form
		assertRedirect(pair.getLeft());
		// data node must not be published and first page not completed
		NodeResponse dataNode = assertSingleDataNode(uuid, n -> {
			assertThat(n.getVersion()).as("Data Node Version").isEqualTo("0.1");
			assertThat(n.getFields().getBooleanField("page1").getValue()).as("Page 1 completed").isFalse();
			assertThat(n.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("someValue");
		});
		// form-data cookie must be present
		assertThat(pair.getRight()).as("Cookie").isPresent();
		assertThat(pair.getRight().get()).as("Cookie").matches(c -> c.value().equals(dataNode.getUuid() + "-page1"), "Cookie contains data node UUID and form page");
	}

	/**
	 * Test posting to a multipage form containing a recaptcha field (which is incorrectly filled)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testMultipageRecaptchaIncorrectlyFilled() throws IOException, InterruptedException {
		// create the form
		String uuid = createForm("multipage_recaptcha.json", true, true);

		// let the recaptcha verification server answer with success: false
		webServer.enqueue(new MockResponse().setBody("{\"success\": false}"));

		// post the input field from the first page and an incorrect recaptcha response
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("page1", "1")
				.addFormDataPart("input1", "someValue")
				.addFormDataPart("g-recaptcha-response", "incorrect")
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody);

		// recaptcha verification server must have answered one request
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(1);
		RecordedRequest request = webServer.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getBody().toString()).as("Request body").isEqualTo("[text=secret=your-secret-key&response=incorrect]");

		// we expect to be redirected back to the form
		assertRedirect(pair.getLeft());
		// data node must not be published and first page is incomplete
		NodeResponse dataNode = assertSingleDataNode(uuid, n -> {
			assertThat(n.getVersion()).as("Data Node Version").isEqualTo("0.1");
			assertThat(n.getFields().getBooleanField("page1").getValue()).as("Page 1 completed").isFalse();
			assertThat(n.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("someValue");
		});
		// form-data cookie must be present
		assertThat(pair.getRight()).as("Cookie").isPresent();
		assertThat(pair.getRight().get()).as("Cookie").matches(c -> c.value().equals(dataNode.getUuid() + "-page1"), "Cookie contains data node UUID and form page");
	}

	/**
	 * Test posting to a multipage form containing a recaptcha field (which is correctly filled)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testMultipageRecaptchaCorrectlyFilled() throws IOException, InterruptedException {
		// create the form
		String uuid = createForm("multipage_recaptcha.json", true, true);

		// let the recaptcha verification server answer with success: true
		webServer.enqueue(new MockResponse().setBody("{\"success\": true}"));

		// post input field from first page and correct recaptcha response
		RequestBody requestBody = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart("page1", "1")
				.addFormDataPart("input1", "someValue")
				.addFormDataPart("g-recaptcha-response", "correct")
				.build();
		Pair<Response, Optional<Cookie>> pair = postToForm(uuid, requestBody);

		// recaptcha verification server must have answered one request
		assertThat(webServer.getRequestCount()).as("Requests to recaptcha").isEqualTo(1);
		RecordedRequest request = webServer.takeRequest(1, TimeUnit.SECONDS);
		assertThat(request.getBody().toString()).as("Request body").isEqualTo("[text=secret=your-secret-key&response=correct]");

		// we expect to be redirected back to the form (for page 2)
		assertRedirect(pair.getLeft());
		// data node must not be published but first page complete
		NodeResponse dataNode = assertSingleDataNode(uuid, n -> {
			assertThat(n.getVersion()).as("Data Node Version").isEqualTo("0.1");
			assertThat(n.getFields().getBooleanField("page1").getValue()).as("Page 1 completed").isTrue();
			assertThat(n.getFields().getStringField("input1").getString()).as("Data Node Field").isEqualTo("someValue");
		});
		// form-data cookie must be present
		assertThat(pair.getRight()).as("Cookie").isPresent();
		assertThat(pair.getRight().get()).as("Cookie").matches(c -> c.value().equals(dataNode.getUuid() + "-page1"), "Value is not empty");
	}

	/**
	 * Test whether a mandatory field is ignored for validation when inside a
	 * conditional container which condition is not met.
	 */
	@Test
	public void testConditionalContainerSkipValidation() throws IOException {
		String uuid = createForm("conditional_container.json", true, true);

		// The submit() method already checks if the request was successful. No further assertions needed here.
		submit(uuid, true, false, false, "checked_input", "ignore_container_contents");
	}

	/**
	 * Test whether validation failed because the mandatory element inside the
	 * conditional container is missing.
	 */
	@Test
	public void testConditionalContainerPerformValidation() throws IOException {
		String uuid = createForm("conditional_container.json", true, true);
		ResponseWithCookies rs = submit(uuid, true, "checked_input", "process_conditional_container");
		PostDataResponse postResponse = JsonUtil.readValue(rs.response.body().string(), PostDataResponse.class);
		JsonObject savedDataFields = loadNode(postResponse.getDataUuid()).map(node -> new JsonObject(node.getFields().toJson())).get();

		assertThat(savedDataFields.fieldNames())
			.as("Saved elements")
			.containsOnly("checked_input", "errors");

		assertThat(new JsonObject(savedDataFields.getString("errors")).fieldNames())
			.as("Validation errors")
			.containsOnly("conditional_input");
	}

	/**
	 * Test whether validation is skipped using nested conditional containers.
	 */
	@Test
	public void testNestedConditionalContainerSkipValidation() throws IOException {
		String uuid = createForm("nested_conditional_container.json", true, true);

		// The submit() method already checks if the request was successful. No further assertions needed here.
		submit(uuid, true, false, false,
			"checked_input_outer", "ignore_container_contents",
			"checked_input_inner", "process_conditional_container");
	}

	/**
	 * Test whether validation failed because the mandatory element inside the
	 * conditional containers is missing.
	 */
	@Test
	public void testNestedConditionalContainerPerformValidation() throws IOException {
		String uuid = createForm("nested_conditional_container.json", true, true);
		ResponseWithCookies rs = submit(uuid, true,
			"checked_input_inner", "process_conditional_container",
			"checked_input_outer", "process_conditional_container");
		PostDataResponse postResponse = JsonUtil.readValue(rs.response.body().string(), PostDataResponse.class);
		JsonObject savedDataFields = loadNode(postResponse.getDataUuid()).map(node -> new JsonObject(node.getFields().toJson())).get();

		assertThat(savedDataFields.fieldNames())
			.as("Saved elements")
			.containsOnly("checked_input_inner", "checked_input_outer", "errors");

		assertThat(new JsonObject(savedDataFields.getString("errors")).fieldNames())
			.as("Validation errors")
			.containsOnly("conditional_input");
	}

	/**
	 * Test that elements inside a conditional container are not saved, when the
	 * checked element is not set to the expected value.
	 */
	@Test
	public void testConditionalContaierSkipSave() throws IOException {
		String uuid = createForm("conditional_container.json", true, true);
		ResponseWithCookies rs = submit(uuid, true,
			"checked_input", "ignore_container_contents",
			"normal_input", "test value",
			"conditional_input", "this should not be saved");
		PostDataResponse postResponse = JsonUtil.readValue(rs.response.body().string(), PostDataResponse.class);
		JsonObject savedDataFields = loadNode(postResponse.getDataUuid()).map(node -> new JsonObject(node.getFields().toJson())).get();

		assertThat(savedDataFields)
			.as("Saved elements")
			.containsOnly(
				Pair.of("normal_input", "test value"),
				Pair.of("checked_input", "ignore_container_contents"),
				Pair.of("errors", ""));
	}

	@Test
	public void testConditionalContainerSkipInMail() throws IOException, MessagingException, InterruptedException {
		String uuid = createForm("conditional_container.json", true, true);

		ResponseWithCookies rs = submit(uuid, true,
			"checked_input", "ignore_container_contents",
			"normal_input", "test value",
			"conditional_input", "this should not be saved");

		assertThat(greenMail.waitForIncomingEmail(MAIL_TIMEOUT, 1))
			.as("Mail sent in time")
			.isTrue();

		MimeMessage[] messages = greenMail.getReceivedMessages();

		assertThat(messages.length)
			.as("Received messages")
			.isEqualTo(1);

		String content = messages[0].getContent().toString().replaceAll("\\r", "");

		System.out.println(content);

		assertThat(content)
			.as("Mail content")
			.isEqualTo(fromFile("conditional_container_mail.html"));
	}

	/**
	 * Test that elements inside a conditional container are saved, when the
	 * checked element is set to the expected value.
	 */
	@Test
	public void testConditionalContaierPerformSave() throws IOException {
		String uuid = createForm("conditional_container.json", true, true);
		ResponseWithCookies rs = submit(uuid, true,
			"checked_input", "process_conditional_container",
			"normal_input", "test value",
			"conditional_input", "this should be saved");
		PostDataResponse postResponse = JsonUtil.readValue(rs.response.body().string(), PostDataResponse.class);
		JsonObject savedDataFields = loadNode(postResponse.getDataUuid()).map(node -> new JsonObject(node.getFields().toJson())).get();

		assertThat(savedDataFields)
			.as("Saved elements")
			.containsOnly(
				Pair.of("normal_input", "test value"),
				Pair.of("checked_input", "process_conditional_container"),
				Pair.of("conditional_input", "this should be saved"),
				Pair.of("errors", ""));
	}

	/**
	 * Post the given body to the form
	 * @param uuid form uuid
	 * @param body post body
	 * @return response
	 * @throws IOException
	 */
	protected Pair<Response, Optional<Cookie>> postToForm(String uuid, RequestBody body) throws IOException {
		Request postRequest = new Request.Builder()
				.url(HttpUrl.get(projectUrl("/forms/" + uuid + "/data"))
					.newBuilder().build())
				.header("Referer", "ref")
				.post(body)
				.build();
		Response response = client().newCall(postRequest).execute();
		Optional<Cookie> cookie = Cookie.parseAll(postRequest.url(), response.headers()).stream().filter(c -> FormDataService.COOKIE_NAME_DATA.equals(c.name()))
				.findFirst();

		return Pair.of(response, cookie);
	}

	/**
	 * Assert that the response is a redirect to "ref"
	 * @param response response
	 */
	protected void assertRedirect(Response response) {
		assertThat(response.isRedirect()).as(String.format("Response redirect %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.header("Location")).as("Location Header").isEqualTo("ref");
	}

	/**
	 * Assert that the response is 205 (Reset Content)
	 * @param response response
	 */
	protected void assertOk(Response response) {
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		assertThat(response.code()).as("Response code").isEqualTo(205);
	}

	/**
	 * Assert that there is a single data node for the form
	 * @param formUuid form uuid
	 * @param asserter optional asserter for the data node
	 * @return data node
	 */
	protected NodeResponse assertSingleDataNode(String formUuid, Consumer<NodeResponse> asserter) {
		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, formUuid));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);

		if (asserter != null) {
			asserter.accept(dataNode);
		}

		return dataNode;
	}
}
