package com.gentics.mesh.plugin.formgenerator;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.plugin.formgenerator.service.SchemaService;
import com.gentics.mesh.plugin.test.common.AbstractPluginLocalIntegrationTest;
import com.gentics.mesh.plugin.test.common.OrientDBMeshLocalServer;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.test.local.MeshLocalServer;
import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetupTest;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Integration tests for the FormGeneratorPlugin
 */
public abstract class AbstractFormGeneratorPluginIntegrationTest extends AbstractPluginLocalIntegrationTest {

	protected final static String PROJECT_NAME = "testproject";
	protected final static String PROJECT_HOSTNAME = "testhost";

	protected final static String API_KEY = "Foo4Lais7Mae3vahngaichae6aifoh9aefeeSoh9phooc0pee3";

	static {
		// Use slf4j instead of JUL
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	@ClassRule
	public static final MeshLocalServer server = new OrientDBMeshLocalServer()
		.withInMemoryMode()
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

		server.getMesh().deployPlugin(FormGeneratorTestPlugin.class, "fg").blockingAwait();
	}

	@Before
	public void setupPlugin() throws InterruptedException {
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "fg");
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
		return String.format("http://%s:%d/api/v1/%s/plugins/%s%s", server.getHostname(), server.getPort(), PROJECT_NAME, "fg", path);
	}

	protected static OkHttpClient client() {
		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.connectTimeout(1, TimeUnit.HOURS).readTimeout(1, TimeUnit.HOURS).writeTimeout(1, TimeUnit.HOURS);
		builder.followRedirects(false);
		return builder.build();
	}

	/**
	 * Clean formgenerator data before test run
	 */
	@Before
	public void setup() {
		List<String> schemasToDelete = Arrays.asList(SchemaService.SCHEMA_NAME, SchemaService.TEMPLATE_SCHEMA_NAME);
		List<String> persistentSchemas = Arrays.asList(SchemaService.PLUGIN_SCHEMA_NAME, SchemaService.SCHEMA_NAME, SchemaService.TEMPLATE_SCHEMA_NAME);

		// delete all form nodes and form template nodes
		NodeListResponse response = call(() -> server.client().findNodes(PROJECT_NAME, new PagingParametersImpl().setPerPage(Long.MAX_VALUE)));
		for (NodeResponse node : response.getData()) {
			if (schemasToDelete.contains(node.getSchema().getName())) {
				call(() -> server.client().deleteNode(PROJECT_NAME, node.getUuid(), new DeleteParametersImpl().setRecursive(true)));
			}
		}

		// remove additional schemas
		SchemaListResponse schemas = call(() -> server.client().findSchemas(new PagingParametersImpl().setPerPage(Long.MAX_VALUE)));
		for (SchemaResponse schema : schemas.getData()) {
			if (schema.getName().startsWith(SchemaService.SCHEMA_NAME) && !persistentSchemas.contains(schema.getName())) {
				call(() -> server.client().deleteSchema(schema.getUuid()));
			}
		}
	}

	/**
	 * Convenience method for {@link #createForm(String, boolean)} which passes <code>true</code>
	 * for <code>expectSuccess</code>.
	 *
	 * @param fileName file name
	 * @return response
	 * @throws IOException
	 */
	protected Response createForm(String fileName) throws IOException {
		return createForm(fileName, true);
	}

	/**
	 * Create a form with data from the given file.
	 * Assert that creation succeeded
	 * @param fileName file name
	 * @param expectSuccess whether the request is expected to be successful
	 * @return response
	 * @throws IOException
	 */
	protected Response createForm(String fileName, boolean expectSuccess) throws IOException {
		Request createRequest = new Request.Builder()
			.url(projectUrl("/forms"))
			.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
			.post(requestBodyFromFile(fileName))
			.build();
		Response response = client().newCall(createRequest).execute();

		assertThat(response.isSuccessful())
			.as(String.format("Response status %d (%s)", response.code(), response.message()))
			.isEqualTo(expectSuccess);

		return response;
	}

	/**
	 * Create a new form from file <code>create.json</code>, and update it with the from in <code>fileName</code>.
	 * @param fileName The JSON file for the update request
	 * @param expectSuccess Whether the update request is expected to succedd
	 * @return The <code>NodeResponse</code> of the update request
	 * @throws IOException
	 */
	protected NodeResponse updateForm(String fileName, boolean expectSuccess) throws IOException {
		Response createResponse = createForm("create.json", true);
		NodeResponse createdNode = JsonUtil.readValue(createResponse.body().string(), NodeResponse.class);
		Request updateRequest = new Request.Builder()
			.url(projectUrl("/forms/" + createdNode.getUuid()))
			.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
			.post(requestBodyFromFile(fileName))
			.build();
		Response response = client().newCall(updateRequest).execute();

		assertThat(response.isSuccessful())
			.as(String.format("Response success %d (%s)", response.code(), response.message()))
			.isEqualTo(expectSuccess);

		if (expectSuccess) {
			NodeResponse updatedNode = JsonUtil.readValue(response.body().string(), NodeResponse.class);

			assertThat(updatedNode)
				.as("Loaded Form Node")
				.isEqualToComparingOnlyGivenFields(createdNode, "uuid");

			return updatedNode;
		}

		// The update request is expected to fail.
		return null;
	}

	/**
	 * Create a template with data from the given file.
	 * Assert that creation succeeded
	 * @param fileName file name
	 * @return response
	 * @throws IOException
	 */
	protected Response createTemplate(String fileName) throws IOException {
		Request createRequest = new Request.Builder()
			.url(projectUrl("/templates"))
			.header(FormGeneratorPlugin.SECRET_HEADER_NAME, API_KEY)
			.post(requestBodyFromFile(fileName))
			.build();
		Response response = client().newCall(createRequest).execute();
		assertThat(response.isSuccessful()).as(String.format("Response success %d (%s)", response.code(), response.message())).isTrue();
		return response;
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
		return IOUtils.toString(getClass().getResourceAsStream(name), "UTF-8");
	}

	/**
	 * Try to load schema with given name
	 * @param name schema name
	 * @return optional schema response
	 */
	protected Optional<SchemaResponse> loadSchema(String name) {
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
}
