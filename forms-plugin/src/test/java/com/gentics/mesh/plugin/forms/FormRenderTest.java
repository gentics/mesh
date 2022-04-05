package com.gentics.mesh.plugin.forms;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.forms.config.MultipageRenderMode;
import com.gentics.mesh.plugin.forms.model.response.FormOnlineResponse;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.test.ResponseWithCookies;
import com.gentics.mesh.util.UUIDUtil;
import io.vertx.core.json.JsonObject;
import okhttp3.Cookie;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for rendering forms
 */
public class FormRenderTest extends AbstractFormsPluginIntegrationTest {

	private static final String CUSTOM_TEMPLATE = "customTemplate";

	@Before
	public void setup() {
		super.setup();
		// activate plugin
		assertThat(activate(PROJECT_NAME)).as("Activation status").isTrue();
	}

	/**
	 * Test rendering a form
	 * @throws IOException
	 */
	@Test
	public void testRender() throws IOException {
		doTestSimpleRender("inputfield", false, null);
	}

	@Test
	public void testRenderPoll() throws IOException {
		doTestSimpleRender("simple-poll", false, null);
	}

	@Test
	public void testRenderTemplateContext() throws IOException {
		doTestSimpleRender("inputfield", false, CUSTOM_TEMPLATE);
	}

	@Test
	public void testRenderEmptyPollTemplateContext() throws IOException {
		doTestSimpleRender("simple-poll", true, CUSTOM_TEMPLATE);
	}

	@Test
	public void testRenderPollResults() throws IOException {
		doTestRenderPollResults(null);
	}

	@Test
	public void testRenderPollResultsTemplateContext() throws IOException {
		doTestRenderPollResults(CUSTOM_TEMPLATE);
	}

	public void doTestRenderPollResults(String templateContext) throws IOException {
		String uuid = createForm("poll-results.json");

		submitMultiple(uuid,
			true,
			Arrays.asList(
				new String[] {
					"checkbox1", "1",
					"radiobuttons1", "r_1",
					"singleselect1", "s_1",
					"multiselect1", "m_1", "multiselect1", "m_2", "multiselect1", "m_3" },
				new String[] {
					"checkbox1", "1",
					"radiobuttons1", "r_1",
					"singleselect1", "s_1",
					"multiselect1", "m_1", "multiselect1", "m_2" },
				new String[] {
					"radiobuttons1", "r_1",
					"singleselect1", "s_2",
					"multiselect1", "m_2", "multiselect1", "m_3" },
				new String[] {
					"radiobuttons1", "r_2",
					"singleselect1", "s_2",
					"multiselect1", "m_2" },
				new String[] {
					"radiobuttons1", "r_1",
					"singleselect1", "s_2",
					"multiselect1", "m_1",
					"multiselect1", "m_3" },
				new String[] {
					"radiobuttons1", "r_2",
					"singleselect1", "s_3",
					"multiselect1", "m_3" },
				new String[] {
					"radiobuttons1", "r_3",
					"singleselect1", "s_3",
					"multiselect1", "m_3" }));

		StringBuilder url = new StringBuilder(projectUrl("/forms/" + uuid));
		StringBuilder expectedResultFilename = new StringBuilder("poll-results");

		if (StringUtils.isNotBlank(templateContext)) {
			url.append("?templateContext=").append(templateContext);
			expectedResultFilename.append("-").append(templateContext);
		}

		Request.Builder builder = new Request.Builder()
			.url(url.toString())
			.get()
			.header("Accept", "text/html");

		builder.header("Cookie", FormDataService.COOKIE_NAME_VOTED + "=" + uuid);

		Response response = client().newCall(builder.build()).execute();

		assertThat(response.isSuccessful())
			.as(String.format("Render response success %d (%s)", response.code(), response.message()))
			.isTrue();
		assertThat(response.body().string())
			.as("Rendered result")
			.isEqualTo(fromFile(expectedResultFilename.append(".html").toString()));
	}

	@Test
	public void testRenderPollUnpublished() throws IOException {
		String uuid = createForm("poll-results.json");
		ResponseWithCookies rs = submit(uuid, false, true, false,
			"checkbox1", "1",
			"radiobuttons1", "r_1",
			"singleselect1", "s_1",
			"multiselect1", "m_1", "multiselect1", "m_2", "multiselect1", "m_3");

		rs = submit(uuid, true, true, false,
			"radiobuttons1", "r_2",
			"singleselect1", "s_1",
			"multiselect1", "m_1", "multiselect1", "m_2", "multiselect1", "m_3");

		Request.Builder builder = new Request.Builder()
			.url(projectUrl("/forms/" + uuid))
			.get()
			.header("Accept", "text/html");

		builder.header("Cookie", FormDataService.COOKIE_NAME_VOTED + "=" + uuid);

		Response response = client().newCall(builder.build()).execute();

		assertThat(response.isSuccessful())
			.as(String.format("Render response success %d (%s)", response.code(), response.message()))
			.isTrue();
		assertThat(response.body().string())
			.as("Rendered result")
			.isEqualTo(fromFile("poll-results-not-published.html"));
	}

	/**
	 * Test rendering a form with language fallback
	 * @throws IOException
	 */
	@Test
	public void testRenderFallback() throws IOException {
		List<String> languages = Arrays.asList("de", "en", "it", "fr");

		// set the languages for fallback
		setLanguages(PROJECT_NAME, languages);

		String uuid = createMultilanguageForm("inputfield_languages_en.json", "inputfield_languages_it.json");

		for (String language : languages) {
			Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + uuid) + "?lang=" + language).header("Accept", "text/html").get().build();
			Response response = client().newCall(renderRequest).execute();
			assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

			assertThat(response.body().string()).as("Rendered form in " + language)
					.isEqualTo(fromFile("inputfield_languages_" + language + ".html"));
		}
	}

	/**
	 * Test rendering a form with data
	 * @throws IOException
	 */
	@Test
	public void testRenderData() throws IOException {
		String uuid = createForm("threefields.json");

		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("input1", "someValue").build();
		okio.Buffer buffer = new okio.Buffer();
		requestBody.writeTo(buffer);

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data"))
				.header("Referer", "ref").post(requestBody).build();
		Response postDataResponse = client().newCall(postRequest).execute();
		assertThat(postDataResponse.isRedirect())
				.as(String.format("Response redirect %d (%s)", postDataResponse.code(), postDataResponse.message())).isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);

		Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data/" + dataNode.getUuid())).header("Accept", "text/html").get().build();
		Response response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("threefields_data.html"));
	}

	/**
	 * Test rendering a multipage-form with data, and default multipage behaviour.
	 * @throws IOException
	 */
	@Test
	public void testRenderDataMultipage() throws IOException {
		doTestRenderDataMultipage(MultipageRenderMode.SKIP_COMPLETED);
	}

	/**
	 * Test rendering a multipage-form with data, and force next page to be the
	 * one after the submitted one.
	 * @throws IOException
	 */
	@Test
	public void testRenderDataMultipageDontSkip() throws IOException {
		doTestRenderDataMultipage(MultipageRenderMode.FORCE_NEXT);
	}

	private void doTestRenderDataMultipage(MultipageRenderMode mode) throws IOException {
		String uuid = createForm("multipage.json");

		// only fill the first page (without error)
		RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
			.addFormDataPart("page1", "1")
			.addFormDataPart("input1", "someValue")
			.build();
		okio.Buffer buffer = new okio.Buffer();
		requestBody.writeTo(buffer);

		Request postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data"))
			.header("Referer", "ref").post(requestBody).build();
		Response postDataResponse = client().newCall(postRequest).execute();
		assertThat(postDataResponse.isRedirect())
			.as(String.format("Response redirect %d (%s)", postDataResponse.code(), postDataResponse.message())).isTrue();

		NodeListResponse dataNodes = call(() -> server.client().findNodeChildren(PROJECT_NAME, uuid));
		assertThat(dataNodes.getData()).as("Data node list").hasSize(1);
		NodeResponse dataNode = dataNodes.getData().get(0);

		// Check the form data cookie has the correct data node UUID and name of the last submitted page.
		assertThat(getFormdataCookie(postDataResponse))
			.isEqualTo(dataNode.getUuid() + "-page1");

		Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data/" + dataNode.getUuid())).header("Accept", "text/html").get().build();
		Response response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("multipage_data_page1.html"));

		// now post page 2 (incomplete)
		requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
			.addFormDataPart("page2", "1")
			.build();
		buffer = new okio.Buffer();
		requestBody.writeTo(buffer);

		postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data/" + dataNode.getUuid()))
			.header("Referer", "ref").post(requestBody).build();
		postDataResponse = client().newCall(postRequest).execute();
		assertThat(postDataResponse.isRedirect())
			.as(String.format("Response redirect %d (%s)", postDataResponse.code(), postDataResponse.message())).isTrue();
		// Check the form data cookie has the correct data node UUID and name of the last submitted page.
		assertThat(getFormdataCookie(postDataResponse))
			.isEqualTo(dataNode.getUuid() + "-page2");

		renderRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data/" + dataNode.getUuid())).header("Accept", "text/html").get().build();
		response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("multipage_data_page1.html"));

		if (mode == MultipageRenderMode.SKIP_COMPLETED) {
			return;
		}

		FormsTestPlugin.instance.configuration.setMultipageRenderMode(MultipageRenderMode.FORCE_NEXT);

		// now post page 2 (now without errors)
		requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
			.addFormDataPart("page2", "1")
			.addFormDataPart("input2", "someOtherValue")
			.build();
		buffer = new okio.Buffer();
		requestBody.writeTo(buffer);

		postRequest = new Request.Builder().url(projectUrl("/forms/" + uuid + "/data/" + dataNode.getUuid()))
			.header("Referer", "ref").post(requestBody).build();
		postDataResponse = client().newCall(postRequest).execute();
		assertThat(postDataResponse.isRedirect())
			.as(String.format("Response redirect %d (%s)", postDataResponse.code(), postDataResponse.message())).isTrue();

		String formdataCookie = getFormdataCookie(postDataResponse);

		// Check the form data cookie has the correct data node UUID and name of the last submitted page.
		assertThat(formdataCookie)
			.isEqualTo(dataNode.getUuid() + "-page2");

		// Check that the render request marks page3 as active
		renderRequest = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/data/" + formdataCookie))
			.header("Accept", "text/html")
			.get()
			.build();
		response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("multipage_data_page2.html"));

		// Fake a render render request where we say page1 was submitted last.
		renderRequest = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/data/" + dataNode.getUuid() + "-page1"))
			.header("Accept", "text/html")
			.get()
			.build();
		response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		// Make sure page2 is marked as active.
		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("multipage_data_page3.html"));
	}

	/**
	 * Get the value for the {@link FormDataService#COOKIE_NAME_DATA form data}
	 * cookie.
	 *
	 * <p>
	 *     If the cookie is not found this method will return an empty string.
	 * </p>
	 *
	 * @param response The response to the form data save request
	 * @return The value of the form data cookie or an empty string if it is missing
	 */
	private String getFormdataCookie(Response response) {
		return Cookie.parseAll(response.request().url(), response.headers()).stream()
			.filter(c -> c.name().equals(FormDataService.COOKIE_NAME_DATA))
			.map(Cookie::value)
			.findFirst()
			.orElse("");

	}

	/**
	 * Test rendering a form preview
	 * @throws IOException
	 */
	@Test
	public void testRenderPreview() throws IOException {
		String uuid = UUIDUtil.randomUUID();
		Request renderPreviewRequest = new Request.Builder()
			.url(projectUrl("/forms/" + uuid + "/preview"))
			.header("Accept", "text/html")
			.post(requestBodyFromFile("inputfield.json"))
			.build();
		Response response = client().newCall(renderPreviewRequest).execute();

		assertThat(response.isSuccessful())
			.as(String.format("Response status %d (%s)", response.code(), response.message()))
			.isEqualTo(true);

		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("inputfield.html"));
	}

	/**
	 * Test default field with custom properties
	 * @throws IOException
	 */
	@Test
	public void testFieldWithProperties() throws IOException {
		String uuid = createForm("inputfieldWithProperties.json");

		NodeResponse nodeResponse = server.client().findNodeByUuid(PROJECT_NAME, uuid).blockingGet();
		JsonObject loadedJson = JsonUtil.readValue(nodeResponse.getFields().getStringField("json").getString(), JsonObject.class);
		JsonObject expectedJson = JsonUtil.readValue(fromFile("inputfieldWithProperties.json"), JsonObject.class);
		assertThat(loadedJson).as("NodeResponse json").isEqualTo(expectedJson);

		Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + uuid)).header("Accept", "text/html").get().build();
		Response response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();

		assertThat(response.body().string()).as("Rendered form").isEqualTo(fromFile("inputfieldWithProperties.html"));
	}

	/**
	 * Test rendering an offline form
	 * @throws IOException
	 */
	@Test
	public void testRenderOfflineForm() throws IOException {
		String uuid = createForm("inputfield.json");

		// set form offline
		FormOnlineResponse formOnlineResponse = setFormOffline(uuid, "en", true, true);
		assertThat(formOnlineResponse).as("Response").isNotNull();
		assertThat(formOnlineResponse.isOnline()).as("Form is online").isFalse();

		// try to render the form (we expect this to fail with 404)
		Request renderRequest = new Request.Builder().url(projectUrl("/forms/" + uuid)).header("Accept", "text/html").get().build();
		Response response = client().newCall(renderRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isFalse();
	}

	/**
	 * Test rendering a multipage form
	 * @throws IOException
	 */
	@Test
	public void testRenderMultipage() throws IOException {
		doTestSimpleRender("multipage", false, null);
	}

	private void doTestSimpleRender(String testName, boolean addVotedCookie, String templateContext) throws IOException {
		String uuid = createForm(testName + ".json");

		StringBuilder url = new StringBuilder(projectUrl("/forms/" + uuid));

		if (StringUtils.isNotBlank(templateContext)) {
			url.append("?templateContext=").append(templateContext);
		}

		Request.Builder builder = new Request.Builder()
			.url(url.toString())
			.get()
			.header("Accept", "text/html");

		if (addVotedCookie) {
			builder.header("Cookie", FormDataService.COOKIE_NAME_VOTED + "=" + uuid);
		}

		Response response = client().newCall(builder.build()).execute();

		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		StringBuilder expectedResultFilename = new StringBuilder(testName);

		if (StringUtils.isNotBlank(templateContext)) {
			expectedResultFilename.append("-").append(templateContext);
		}

		assertThat(response.body().string())
			.as("Rendered form")
			.isEqualTo(fromFile(expectedResultFilename.append(".html").toString()));
	}
}
