package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.plugin.forms.model.LanguagesModel;
import com.gentics.mesh.plugin.forms.model.response.FormOnlineResponse;
import com.gentics.mesh.plugin.forms.model.response.StatusResponse;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.plugin.forms.service.SchemaService;
import com.gentics.mesh.plugin.forms.test.ResponseWithCookies;
import com.gentics.mesh.plugin.test.common.AbstractPluginLocalIntegrationTest;
import com.gentics.mesh.plugin.test.common.HibernateMeshLocalServer;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.test.local.MeshLocalServer;
import com.gentics.mesh.util.URIUtils;
import com.gentics.mesh.util.UUIDUtil;
import com.google.common.net.HttpHeaders;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Single;
import okhttp3.Cookie;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Integration tests for the FormsPlugin
 */
public abstract class AbstractFormsPluginIntegrationTest extends AbstractPluginLocalIntegrationTest {

	public static final String FIELD_RS_DATA_UUID = "dataUuid";
	public static final String FIELD_RS_CREATED = "created";

	protected final static String PROJECT_NAME = "testproject";
	protected final static String PROJECT_HOSTNAME = "testhost";
	protected final static String PROJECT_PATH_PREFIX = "/testprefix";

	@ClassRule
	public static final MeshLocalServer server = new HibernateMeshLocalServer()
		.waitForStartup();

	@Rule
	public GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

	@BeforeClass
	public static void setupOnce() throws InterruptedException, ExecutionException, TimeoutException {
		ProjectCreateRequest createProject = new ProjectCreateRequest()
			.setName(PROJECT_NAME)
			.setSchemaRef("folder");

		createProject.setHostname(PROJECT_HOSTNAME);

		call(() -> server.client().createProject(createProject));

		server.getMesh().deployPlugin(FormsTestPlugin.class, "forms").blockingAwait();
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "forms");
	}

	/**
	 * Get the project specific url for the plugin with the given path appended
	 * @param path path
	 * @return complete url
	 */
	protected static String projectUrl(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return String.format("http://%s:%d/api/v1/%s/plugins/%s%s", server.getHostname(), server.getPort(), PROJECT_NAME, "forms", path);
	}

	/**
	 * Get the auth headers (based on whether the permission flag is set)
	 * @param permission flag
	 * @return auth headers or empty headers (if permission flag is not set)
	 */
	protected static Headers auth(boolean permission) {
		return Headers.of(permission ? server.client().getAuthentication().getHeaders() : Collections.emptyMap());
	}

	/**
	 * Assert that the response was 403
	 * @param response response
	 */
	protected static void assertForbidden(Response response) {
		assertThat(response.isSuccessful())
				.as(String.format("Response status %d (%s)", response.code(), response.message())).isEqualTo(false);
		assertThat(response.code()).as("Response code").isEqualTo(403);
	}

	/**
	 * Assert that the response was 404
	 * @param response response
	 */
	protected static void assertNotFound(Response response) {
		assertThat(response.isSuccessful())
				.as(String.format("Response status %d (%s)", response.code(), response.message())).isEqualTo(false);
		assertThat(response.code()).as("Response code").isEqualTo(404);
	}

	protected static OkHttpClient client() {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.connectTimeout(1, TimeUnit.HOURS).readTimeout(1, TimeUnit.HOURS).writeTimeout(1, TimeUnit.HOURS);
		builder.followRedirects(false);
		return builder.build();
	}

	/**
	 * Perform the request, assert that the request was successful, if the user had permission and the expectSuccess flag is true.
	 * Return the response as given class
	 * @param <T> response type
	 * @param request request to perform
	 * @param permission true if the user has permission
	 * @param expectSuccess true iff success is expected
	 * @param clazz response type
	 * @return response (may be null, if the request failed)
	 * @throws IOException
	 */
	protected <T> T doRequest(Request request, boolean permission, boolean expectSuccess, Class<T> clazz)
			throws IOException {
		Response response = client().newCall(request).execute();

		if (permission) {
			assertThat(response.isSuccessful())
					.as(String.format("Response status %d (%s)", response.code(), response.message()))
					.isEqualTo(expectSuccess);
			if (expectSuccess) {
				return JsonUtil.readValue(response.body().string(), clazz);
			} else {
				return null;
			}
		} else {
			assertForbidden(response);
			return null;
		}
	}

	/**
	 * Clean forms data before test run
	 */
	@Before
	public void setup() {
		List<String> schemasToDelete = Arrays.asList(SchemaService.PLUGIN_SCHEMA_NAME);

		// delete all form nodes and form template nodes
		NodeListResponse response = call(() -> server.client().findNodes(PROJECT_NAME, new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		for (NodeResponse node : response.getData()) {
			if (schemasToDelete.contains(node.getSchema().getName())) {
				call(() -> server.client().deleteNode(PROJECT_NAME, node.getUuid(), new DeleteParametersImpl().setRecursive(true)));
			}
		}

		// remove additional schemas
		SchemaListResponse schemas = call(() -> server.client().findSchemas(new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE)));
		for (SchemaResponse schema : schemas.getData()) {
			if (schema.getName().startsWith(SchemaService.SCHEMA_NAME)) {
				call(() -> server.client().deleteSchema(schema.getUuid()));
			}
		}
	}

	/**
	 * Convenience method for {@link #createForm(String, boolean, boolean)} which passes <code>true</code>
	 * for <code>expectSuccess</code>.
	 *
	 * @param fileName file name
	 * @return form uuid (null if request fails)
	 * @throws IOException
	 */
	protected String createForm(String fileName) throws IOException {
		return createForm(fileName, true, true);
	}

	/**
	 * Create a form with data from the given file.
	 * Assert that creation succeeded
	 * @param fileName file name
	 * @param permission whether the request shall be made with the required permissions
	 * @param expectSuccess whether the request is expected to be successful
	 * @return form uuid (null if request fails)
	 * @throws IOException
	 */
	protected String createForm(String fileName, boolean permission, boolean expectSuccess) throws IOException {
		String uuid = UUIDUtil.randomUUID();

		JsonNode response = doRequest(new Request.Builder()
				.url(projectUrl("/forms/" + uuid))
				.headers(auth(permission))
				.post(requestBodyFromFile(fileName))
				.build(), permission, expectSuccess, JsonNode.class);

		if (permission && expectSuccess) {
			assertThat(response).as("Response").isNotNull();
			assertThat(response.get("uuid").asText()).as("Returned uuid").isEqualTo(uuid);
			return uuid;
		} else {
			call(() -> server.client().findNodeByUuid(PROJECT_NAME, uuid,
					new VersioningParametersImpl().setVersion("draft")), HttpResponseStatus.NOT_FOUND);
			return null;
		}
	}

	/**
	 * Create a form with data from the given files, which are supposed to be the language variants
	 * Assert that creation succeeded
	 * @param fileNames file names
	 * @return uuid
	 * @throws IOException
	 */
	protected String createMultilanguageForm(String...fileNames) throws IOException {
		String uuid = UUIDUtil.randomUUID();
		Response response = null;

		for (String fileName : fileNames) {
			Request createRequest = new Request.Builder().url(projectUrl("/forms/" + uuid)).headers(auth(true))
					.post(requestBodyFromFile(fileName)).build();
			response = client().newCall(createRequest).execute();

			assertThat(response.isSuccessful())
					.as(String.format("Response status %d (%s)", response.code(), response.message())).isEqualTo(true);
		}

		return uuid;
	}

	/**
	 * Create a new form from file <code>create.json</code>, and update it with the from in <code>fileName</code>.
	 * @param fileName The JSON file for the update request
	 * @param permission TODO
	 * @param expectSuccess Whether the update request is expected to succeed
	 * @param formHandlers optional form handlers, which will be called with the form uuid before the form is updated
	 * @return The <code>NodeResponse</code> of the update request
	 * @throws IOException
	 */
	@SafeVarargs
	protected final NodeResponse updateForm(String fileName, boolean permission, boolean expectSuccess, Consumer<String>...formHandlers) throws IOException {
		String uuid = createForm("create.json", true, true);
		Optional<SchemaResponse> formSchema = loadSchema(SchemaService.getSchemaName(uuid));

		assertThat(formSchema).as("Form Schema").isPresent();

		// Do not unassign the schema if the update request is not expected to succeed, since this will cause problems in the assertions later on.
		if (expectSuccess) {
			call(() -> server.client().unassignSchemaFromProject(PROJECT_NAME, formSchema.get().getUuid()));
		}

		for (Consumer<String> handler : formHandlers) {
			handler.accept(uuid);
		}

		Request updateRequest = new Request.Builder()
			.url(projectUrl("/forms/" + uuid))
			.headers(auth(permission))
			.post(requestBodyFromFile(fileName))
			.build();
		Response response = client().newCall(updateRequest).execute();

		if (permission) {
			assertThat(response.isSuccessful())
					.as(String.format("Response success %d (%s)", response.code(), response.message()))
					.isEqualTo(expectSuccess);

			assertThat(loadSchema(SchemaService.getSchemaName(uuid))).as("Update form Schema").isPresent();

			if (expectSuccess) {
				NodeResponse updatedNode = JsonUtil.readValue(response.body().string(), NodeResponse.class);

				return updatedNode;
			}
		} else {
			assertForbidden(response);
		}

		// The update request is expected to fail.
		return null;
	}

	/**
	 * Get the contents of the given file (which must be in json format) as RequestBody
	 * @param name file name
	 * @return RequestBody
	 * @throws IOException
	 */
	protected RequestBody requestBodyFromFile(String name) throws IOException {
		return RequestBody.create(MediaType.parse("application/json"), fromFile(name));
	}

	/**
	 * Get the contents of the given file
	 * @param name file name
	 * @return contents
	 * @throws IOException
	 */
	protected String fromFile(String name) throws IOException {
		InputStream contents = getClass().getResourceAsStream(name);

		if (contents == null) {
			throw new FileNotFoundException("File not found: " + name);
		}

		return IOUtils.toString(contents, "UTF-8");
	}

	/**
	 * Try to load schema with given name and assigned to {@link #PROJECT_NAME}.
	 * @param name schema name
	 * @return optional schema response
	 */
	protected Optional<SchemaResponse> loadSchema(String name) {
		return call(() -> server.client().findSchemas(PROJECT_NAME, new PagingParametersImpl().setPerPage((long) Integer.MAX_VALUE))).getData().stream()
			.filter(schema -> name.equals(schema.getName())).findFirst().map(e -> Optional.of(e)).orElse(Optional.empty());
	}

	/**
	 * Try to load schema with given name globally
	 * @param name schema name
	 * @return optional schema response
	 */
	protected Optional<SchemaResponse> loadGlobalSchema(String name) {
		return call(() -> server.client().findSchemas(new PagingParametersImpl().setPerPage(Long.MAX_VALUE))).getData().stream()
			.filter(schema -> name.equals(schema.getName())).findFirst().map(e -> Optional.of(e)).orElse(Optional.empty());
	}

	/**
	 * Try to load node with given uuid
	 * @param uuid uuid
	 * @return optional node response
	 */
	protected Optional<NodeResponse> loadNode(String uuid) {
		Single<NodeResponse> future = server.client().findNodeByUuid(PROJECT_NAME, uuid).toSingle();
		Single<Optional<NodeResponse>> fut = future
			.map(n -> Optional.of(n))
			.onErrorReturnItem(Optional.empty());
		return fut.blockingGet();
	}

	/**
	 * Await completion of the completable
	 * @param completable completable
	 * @param timeout timeout
	 * @param unit unit of the timeout
	 * @throws InterruptedException
	 */
	protected void await(Completable completable, long timeout, TimeUnit unit) throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		completable.doOnComplete(() -> latch.countDown()).subscribe();
		latch.await(timeout, unit);
	}

	/**
	 * Check whether the form is online
	 * @param uuid form uuid
	 * @param language language
	 * @param permission true when the user has permission to do the call
	 * @param expectSuccess true if success is expected
	 * @return response (may be null, if the call fails)
	 * @throws IOException
	 */
	protected FormOnlineResponse checkFormOnline(String uuid, String language, boolean permission, boolean expectSuccess) throws IOException {
		return doRequest(new Request.Builder().url(projectUrl("/forms/" + uuid + "/online/" + language))
				.headers(auth(permission)).get().build(), permission, expectSuccess, FormOnlineResponse.class);
	}

	/**
	 * Set the form offline
	 * @param uuid form uuid
	 * @param language language
	 * @param permission true for permission
	 * @param expectSuccess true for expected success
	 * @return response (may be null)
	 * @throws IOException
	 */
	protected FormOnlineResponse setFormOffline(String uuid, String language, boolean permission, boolean expectSuccess) throws IOException {
		return doRequest(
				new Request.Builder().url(projectUrl("/forms/" + uuid + "/online/" + language))
						.headers(auth(permission)).delete().build(),
				permission, expectSuccess, FormOnlineResponse.class);
	}

	/**
	 * Set the form online
	 * @param uuid form uuid
	 * @param language language
	 * @param permission true for permission
	 * @param expectSuccess true for expected success
	 * @return response (may be null)
	 * @throws IOException
	 */
	protected FormOnlineResponse setFormOnline(String uuid, String language, boolean permission, boolean expectSuccess) throws IOException {
		return doRequest(
				new Request.Builder().url(projectUrl("/forms/" + uuid + "/online/" + language))
						.headers(auth(permission)).put(RequestBody.create(MediaType.get("text/plain"), "")).build(),
				permission, expectSuccess, FormOnlineResponse.class);
	}

	/**
	 * Get the activation status for the project
	 * @param project project
	 * @return true iff plugin is activated for project
	 */
	protected boolean getStatus(String project) {
		StatusResponse response = call(() -> server.client()
				.get(String.format("/%s/plugins/forms/active", URIUtils.encodeSegment(project)), StatusResponse.class));
		return response.isActive();
	}

	/**
	 * Activate the plugin for the project
	 * @param project project
	 * @return true iff plugin is activated for the project
	 */
	protected boolean activate(String project) {
		StatusResponse response = call(() -> server.client()
				.put(String.format("/%s/plugins/forms/active", URIUtils.encodeSegment(project)), StatusResponse.class));
		return response.isActive();
	}

	/**
	 * Deactivate the plugin for the project
	 * @param project project
	 * @return true iff plugin is activated for the project
	 */
	protected boolean deActivate(String project) {
		StatusResponse response = call(() -> server.client()
				.delete(String.format("/%s/plugins/forms/active", URIUtils.encodeSegment(project)), StatusResponse.class));
		return response.isActive();
	}

	/**
	 * Get the languages for the project
	 * @param project project
	 * @return list of languages (may be null or empty)
	 */
	protected List<String> getLanguages(String project) {
		LanguagesModel response = call(() -> server.client().get(
				String.format("/%s/plugins/forms/languages", URIUtils.encodeSegment(project)), LanguagesModel.class));
		return response.getLanguages();
	}

	/**
	 * Set the languages for the project
	 * @param project project
	 * @param languages language code list
	 * @return list of languages (may be null or empty)
	 */
	protected List<String> setLanguages(String project, List<String> languages) {
		LanguagesModel response = call(
				() -> server.client().put(String.format("/%s/plugins/forms/languages", URIUtils.encodeSegment(project)),
						new LanguagesModel().setLanguages(languages), LanguagesModel.class));
		return response.getLanguages();
	}

	protected void submitMultiple(String formUuid, boolean expectRedirect, List<String[]> data) throws IOException {
		for (String[] vote : data) {
			ResponseWithCookies rs = submit(formUuid, true, expectRedirect, false, vote);

			if (expectRedirect) {
				assertThat(rs.response.isRedirect())
					.as(String.format("Response is redirect %d (%s)", rs.response.code(), rs.response.message()))
					.isTrue();
			} else {
				assertThat(rs.response.isSuccessful())
					.as("Submit response success %d (%s)", rs.response.code(), rs.response.message())
					.isTrue();
			}
		}
	}

	protected ResponseWithCookies submit(String formUuid, String ...data) throws IOException {
		return submit(formUuid, true, false, data);
	}

	protected ResponseWithCookies submit(String formUuid, boolean json, String ...data) throws IOException {
		return submit(formUuid, true, false, false, true, data);
	}

	protected ResponseWithCookies submit(String formUuid, boolean publish, boolean expectRedirect, String ...data) throws IOException {
		return submit(formUuid, publish, expectRedirect, false, data);
	}

	protected ResponseWithCookies submit(String formUuid, boolean publish, boolean expectRedirect, boolean cookie, String ...data) throws IOException {
		return submit(formUuid, publish, expectRedirect, cookie, false, data);
	}

	protected ResponseWithCookies submit(String formUuid, boolean publish, boolean expectRedirect, boolean cookie, boolean json, String ...data) throws IOException {
		MultipartBody.Builder body = new MultipartBody.Builder()
			.setType(MultipartBody.FORM);

		for (int ii = cookie ? 1 : 0; ii < data.length; ii++) {
			String field = data[ii++];
			String value = ii < data.length ? data[ii] : "";

			body.addFormDataPart(field, value);
		}

		String publishParam = publish ? "" : "?publish=false";
		Request.Builder requestBuilder = new Request.Builder()
			.url(projectUrl(String.format("/forms/%s/%sdata%s", formUuid, (json ? "json" : ""), publishParam)))
			.header("Referer", "ref");

		if (cookie) {
			requestBuilder.header(HttpHeaders.COOKIE, data[0]);
		}

		Request postRequest = requestBuilder.post(body.build()).build();
		Response response = client().newCall(postRequest).execute();

		if (expectRedirect) {
			assertThat(response.isRedirect())
				.as(String.format("Response is redirect %d (%s)", response.code(), response.message()))
				.isTrue();
		} else {
			assertThat(response.isSuccessful())
				.as(String.format("Response success %d (%s)", response.code(), response.message()))
				.isTrue();

			assertThat(response.code())
				.as("Response code")
				.isEqualTo(json ? HttpResponseStatus.OK.code() : HttpResponseStatus.RESET_CONTENT.code());
		}

		return new ResponseWithCookies(response, Cookie.parseAll(postRequest.url(), response.headers()));
	}

	/**
	 * Get the CSV records from the input export as a set of lines.
	 * @param export The export.
	 * @return The entries in the CSV export.
	 */
	protected Set<String> getExportRecords(String export) throws IOException {
		Set<String> records = new HashSet<>();

		try (BufferedReader in = new BufferedReader(new StringReader(export))) {
			String line;

			while ((line = in.readLine()) != null) {
				records.add(line);
			}
		}

		return records;
	}

	/**
	 * Get the list of form data nodes for the given form
	 * @param formUuid form UUID
	 * @return list of form data nodes
	 */
	protected List<NodeResponse> getFormDataNodes(String formUuid) {
		NodeListResponse formNodeChildren = call(() -> server.client().findNodeChildren(PROJECT_NAME, formUuid));
		Optional<NodeResponse> optDataFolder = formNodeChildren.getData().stream()
				.filter(n -> StringUtils.equals(n.getDisplayName(), FormService.DATA_FOLDER_NAME)).findFirst();
		assertThat(optDataFolder).as("Form Data Folder").isNotEmpty();

		NodeListResponse formDataFolderChildren = call(() -> server.client().findNodeChildren(PROJECT_NAME, optDataFolder.get().getUuid()));
		return formDataFolderChildren.getData().stream()
				.filter(n -> StringUtils.equals(n.getSchema().getName(), "form_" + formUuid))
				.collect(Collectors.toList());
	}

	/**
	 * Get the one existing form data node for the given form. This method will fail, if not exactly one form data node exists.
	 * @param formUuid form UUID
	 * @return existing single form data node
	 */
	protected NodeResponse getSingleFormDataNode(String formUuid) {
		List<NodeResponse> dataNodes = getFormDataNodes(formUuid);

		assertThat(dataNodes)
			.as("Data node list")
			.hasSize(1);

		return dataNodes.get(0);
	}

	/**
	 * Load the form data with the given UUID.
	 * @param formDataUuid The form data UUID.
	 * @return The loaded form data node.
	 */
	protected NodeResponse getFormDataNode(String formDataUuid) {
		return call(() -> server.client().findNodeByUuid(PROJECT_NAME, formDataUuid));
	}
}
