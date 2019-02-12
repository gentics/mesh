package com.gentics.mesh.test.context;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.util.TestUtils.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;

import com.gentics.mesh.cli.BootstrapInitializerImpl;
import com.gentics.mesh.cli.CoreVerticleLoader;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.router.ProjectsRouter;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.test.TestDataProvider;
import com.gentics.mesh.test.util.TestUtils;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.RoutingContext;
import okhttp3.OkHttpClient;

public abstract class AbstractMeshTest implements TestHelperMethods, TestHttpMethods {

	static {
		// Use slf4j instead of JUL
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	private OkHttpClient httpClient;

	private Map<CompletableFuture<JsonObject>, MeshEvent> futures = new HashMap<>();

	@Rule
	@ClassRule
	public static MeshTestContext testContext = new MeshTestContext();

	private static <T> Maybe<T> fromFuture(Consumer<Future<T>> consumer) {
		return Maybe.create(sub -> {
			Future<T> future = Future.future();
			future.setHandler(result -> {
				if (result.succeeded()) {
					if (result.result() == null) {
						sub.onComplete();
					} else {
						sub.onSuccess(result.result());
					}
				} else {
					sub.onError(result.cause());
				}
			});
			consumer.accept(future);
		});
	}

	private static <T> Completable fromFutureCompletable(Consumer<Future<T>> consumer) {
		return fromFuture(consumer).ignoreElement();
	}

	@Override
	public MeshTestContext getTestContext() {
		return testContext;
	}

	@After
	public void clearLatches() {
		futures.clear();
	}

	@After
	public void checkConsistency() {
		try (Tx tx = tx()) {
			ConsistencyCheckResponse response = new ConsistencyCheckResponse();
			for (ConsistencyCheck check : ConsistencyCheckHandler.getChecks()) {
				ConsistencyCheckResult result = check.invoke(db(), tx, false);
				response.getInconsistencies().addAll(result.getResults());
			}

			assertThat(response.getInconsistencies()).as("Inconsistencies").isEmpty();
		}
	}

	public OkHttpClient httpClient() {
		if (this.httpClient == null) {
			int timeout = 240;
			this.httpClient = new OkHttpClient.Builder()
				.writeTimeout(timeout, TimeUnit.SECONDS)
				.readTimeout(timeout, TimeUnit.SECONDS)
				.connectTimeout(timeout, TimeUnit.SECONDS)
				.build();
		}
		return this.httpClient;
	}

	/**
	 * Drop all indices and create a new index using the current data.
	 *
	 * @throws Exception
	 */
	protected void recreateIndices() throws Exception {
		// We potentially modified existing data thus we need to drop all indices and create them and reindex all data
		MeshInternal.get().searchProvider().clear().blockingAwait();
		for (IndexHandler<?> handler : MeshInternal.get().indexHandlerRegistry().getHandlers()) {
			handler.init().blockingAwait();
			handler.syncIndices().blockingAwait();
		}
	}

	public String getJson(Node node) throws Exception {
		InternalActionContext ac = mockActionContext("lang=en&version=draft");
		ac.data().put(ProjectsRouter.PROJECT_CONTEXT_KEY, TestDataProvider.PROJECT_NAME);
		return node.transformToRest(ac, 0).blockingGet().toJson();
	}

	protected void testPermission(GraphPermission perm, MeshCoreVertex<?, ?> element) {
		RoutingContext rc = tx(() -> mockRoutingContext());

		try (Tx tx = tx()) {
			role().grantPermissions(element, perm);
			tx.success();
		}

		try (Tx tx = tx()) {
			assertTrue("The role {" + role().getName() + "} does not grant permission on element {" + element.getUuid()
				+ "} although we granted those permissions.", role().hasPermission(perm, element));
			assertTrue("The user has no {" + perm.getRestPerm().getName() + "} permission on node {" + element.getUuid() + "/" + element.getClass()
				.getSimpleName() + "}", getRequestUser().hasPermission(element, perm));
		}

		try (Tx tx = tx()) {
			role().revokePermissions(element, perm);
			rc.data().clear();
			tx.success();
		}

		try (Tx tx = tx()) {
			boolean hasPerm = role().hasPermission(perm, element);
			assertFalse("The user's role {" + role().getName() + "} still got {" + perm.getRestPerm().getName() + "} permission on node {" + element
				.getUuid() + "/" + element.getClass().getSimpleName() + "} although we revoked it.", hasPerm);

			hasPerm = getRequestUser().hasPermission(element, perm);
			assertFalse("The user {" + getRequestUser().getUsername() + "} still got {" + perm.getRestPerm().getName() + "} permission on node {"
				+ element.getUuid() + "/" + element.getClass().getSimpleName() + "} although we revoked it.", hasPerm);
		}
	}

	/**
	 * Return the graphql query for the given name.
	 *
	 * @param name
	 * @return
	 * @throws IOException
	 */
	protected String getGraphQLQuery(String name) throws IOException {
		return IOUtils.toString(getClass().getResourceAsStream("/graphql/" + name));
	}

	/**
	 * Return the es text for the given name.
	 *
	 * @param name
	 * @return
	 * @throws IOException
	 */
	protected String getESText(String name) throws IOException {
		return IOUtils.toString(getClass().getResourceAsStream("/elasticsearch/" + name));
	}

	/**
	 * Returns the text string of the resource with the given path.
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	protected String getText(String path) throws IOException {
		return IOUtils.toString(getClass().getResourceAsStream(path));
	}

	/**
	 * Returns the json for the given path.
	 *
	 * @param path
	 * @return
	 * @throws IOException
	 */
	protected JsonObject getJson(String path) throws IOException {
		return new JsonObject(IOUtils.toString(getClass().getResourceAsStream(path)));
	}

	/**
	 * Load the resource and return the buffer with the data.
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	protected Buffer getBuffer(String path) throws IOException {
		InputStream ins = getClass().getResourceAsStream(path);
		assertNotNull("The resource for path {" + path + "} could not be found", ins);
		byte[] bytes = IOUtils.toByteArray(ins);
		return Buffer.buffer(bytes);
	}

	/**
	 * Execute the action and check that the jobs are executed and yields the given status.
	 *
	 * @param action
	 *            Action to be invoked. This action should trigger the migrations
	 * @param status
	 *            Expected job status for all migrations. No assertion will be performed when the status is null
	 * @param expectedJobs
	 *            Amount of expected jobs
	 * @return Migration status
	 */
	protected JobListResponse waitForJobs(Runnable action, MigrationStatus status, int expectedJobs) {
		// Load a status just before the action
		JobListResponse before = call(() -> client().findJobs());

		// Invoke the action
		action.run();

		// Now poll the migration status and check the response
		final int MAX_WAIT = 120;
		for (int i = 0; i < MAX_WAIT; i++) {
			JobListResponse response = call(() -> client().findJobs());
			if (response.getMetainfo().getTotalCount() == before.getMetainfo().getTotalCount() + expectedJobs) {
				if (status != null) {
					boolean allMatching = true;
					for (JobResponse info : response.getData()) {
						if (!status.equals(info.getStatus())) {
							allMatching = false;
						}
					}
					if (allMatching) {
						return response;
					}
				}
			}
			if (i > 30) {
				System.out.println(response.toJson());
			}
			if (i == MAX_WAIT - 1) {
				throw new RuntimeException("Migration did not complete within " + MAX_WAIT + " seconds");
			}
			sleep(1000);
		}
		return null;
	}

	protected void waitForLatestJob(Runnable action) {
		waitForLatestJob(action, MigrationStatus.COMPLETED);
	}

	protected void waitForLatestJob(Runnable action, MigrationStatus status) {
		// Load a status just before the action
		JobListResponse before = call(() -> client().findJobs());

		// Invoke the action
		action.run();

		// Now poll the migration status and check the response
		final int MAX_WAIT = 120;
		for (int i = 0; i < MAX_WAIT; i++) {
			JobListResponse response = call(() -> client().findJobs());
			List<JobResponse> diff = TestUtils.difference(response.getData(), before.getData(), JobResponse::getUuid);
			if (diff.size() > 1) {
				System.out.println(response.toJson());
				throw new RuntimeException("More jobs than expected");
			}
			if (diff.size() == 1) {
				JobResponse newJob = diff.get(0);
				if (newJob.getStatus().equals(status)) {
					return;
				}
			}

			if (i > 2) {
				System.out.println(response.toJson());
			}

			if (i == MAX_WAIT - 1) {
				throw new RuntimeException("Migration did not complete within " + MAX_WAIT + " seconds");
			}
			sleep(1000);
		}
	}

	/**
	 * Execute the action and check that the migration is executed and yields the given status.
	 *
	 * @param action
	 *            Action to be invoked. This action should trigger the jobs
	 * @param status
	 *            Expected job status
	 * @return Job status
	 */
	protected JobResponse waitForJob(Runnable action, String jobUuid, MigrationStatus status) {
		// Invoke the action
		action.run();

		// Now poll the migration status and check the response
		final int MAX_WAIT = 120;
		for (int i = 0; i < MAX_WAIT; i++) {
			JobResponse response = call(() -> client().findJobByUuid(jobUuid));

			if (response.getStatus().equals(status)) {
				return response;
			}

			if (i > 30) {
				System.out.println(response.toJson());
			}

			if (i == MAX_WAIT - 1) {
				throw new RuntimeException("Job did not complete within " + MAX_WAIT + " seconds");
			}
			sleep(1000);
		}

		return null;

	}

	/**
	 * Inform the job worker that new jobs have been enqueued and block until all jobs complete or the timeout has been reached.
	 *
	 * @param jobUuid
	 *            Uuid of the job we should wait for
	 *
	 */
	protected JobListResponse triggerAndWaitForJob(String jobUuid) {
		return triggerAndWaitForJob(jobUuid, COMPLETED);
	}

	/**
	 * Inform the job worker that new jobs are enqueued and check the migration status. This method will block until the migration finishes or a timeout has
	 * been reached.
	 *
	 * @param jobUuid
	 *            Uuid of the job we should wait for
	 * @param status
	 *            Expected status for all jobs
	 */
	protected JobListResponse triggerAndWaitForJob(String jobUuid, MigrationStatus status) {
		waitForJob(() -> {
			MeshEvent.triggerJobWorker();
		}, jobUuid, status);
		return call(() -> client().findJobs());
	}

	protected void triggerAndWaitForAllJobs(MigrationStatus expectedStatus) {
		MeshEvent.triggerJobWorker();

		// Now poll the migration status and check the response
		final int MAX_WAIT = 120;
		for (int i = 0; i < MAX_WAIT; i++) {
			JobListResponse response = call(() -> client().findJobs(new PagingParametersImpl().setPerPage(200L)));

			boolean allDone = true;
			for (JobResponse info : response.getData()) {
				if (!info.getStatus().equals(expectedStatus)) {
					allDone = false;
				}
			}
			if (allDone) {
				break;
			}

			if (i > 30) {
				System.out.println(response.toJson());
			}

			if (i == MAX_WAIT - 1) {
				throw new RuntimeException("Job did not complete within " + MAX_WAIT + " seconds");
			}
			sleep(1000);
		}

	}

	/**
	 * Checks if there are too many additional file handles open after the action has been run.
	 * 
	 * @param action
	 *            Action to be called
	 */
	protected void assertClosedFileHandleDifference(int maximumDifference, Action action) throws Exception {
		Set<String> before = getOpenFiles();
		action.run();
		Set<String> after = getOpenFiles();
		if (after.size() - before.size() > maximumDifference) {
			String info = after.stream().filter(e -> !before.contains(e)).reduce("", (a, b) -> a += "\n" + b);
			throw new RuntimeException(String.format(
				"File handles were not closed properly: Expected max. %d additional handles, got %d Encountered the following new open files\n %s",
				maximumDifference, after.size() - before.size(), info));
		}
	}

	/**
	 * Returns a set of open files.
	 * 
	 * @return Set of open files
	 * @throws IOException
	 */
	public Set<String> getOpenFiles() throws IOException {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		int i = name.indexOf("@");
		if (i > 0) {
			String pid = name.substring(0, i);
			String path = "/proc/" + pid + "/fd";
			Set<String> openFiles = Files
				.list(Paths.get(path))
				.map(this::resolvePath)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet());
			return openFiles;
		} else {
			throw new RuntimeException("Could not get file handle count");
		}
	}

	private Optional<String> resolvePath(Path path) {
		try {
			return Optional.of(path.toRealPath().toString());
		} catch (IOException e) {
			Optional<String> o = Optional.empty();
			return o;
		}
	}

	protected int uploadImage(Node node, String languageTag, String fieldname, String filename, String contentType) throws IOException {
		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(bytes);
		return upload(node, buffer, languageTag, fieldname, filename, contentType);
	}

	protected int upload(Node node, Buffer buffer, String languageTag, String fieldname, String filename, String contentType) throws IOException {
		String uuid = node.getUuid();
		VersionNumber version = node.getGraphFieldContainer(languageTag).getVersion();
		NodeResponse response = call(() -> client().updateNodeBinaryField(PROJECT_NAME, uuid, languageTag, version.toString(), fieldname,
			new ByteArrayInputStream(buffer.getBytes()), buffer.length(),
			filename, contentType));
		assertNotNull(response);
		return buffer.length();
	}

	/**
	 * Wait until the given event has been received.
	 * 
	 * @param address
	 * @param code
	 * @throws TimeoutException
	 */
	protected void waitForEvent(String address, Action code) throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		vertx().eventBus().consumer(address, handler -> {
			latch.countDown();
		});
		code.run();
		latch.await(2000, TimeUnit.SECONDS);
	}

	/**
	 * Wait until the given event has been received.
	 *
	 * @param event
	 * @param code
	 * @throws TimeoutException
	 */
	protected void waitForEvent(MeshEvent event, Action code) throws Exception {
		waitForEvent(event.address, code);
	}

	public ElasticSearchProvider getProvider() {
		return ((ElasticSearchProvider) searchProvider());
	}

	/**
	 * Create a new branch
	 * 
	 * @param name
	 *            branch name
	 * @param latest
	 *            true to make branch the latest
	 * @return new branch
	 */
	protected Branch createBranch(String name, boolean latest) {
		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(name);

		if (latest) {
			request.setLatest(latest);
		}

		return createBranch(request);
	}

	/**
	 * Create a branch with the given request
	 * 
	 * @param request
	 *            request
	 * @return new branch
	 */
	protected Branch createBranch(BranchCreateRequest request) {
		StringBuilder uuid = new StringBuilder();
		waitForJobs(() -> {
			BranchResponse response = call(() -> client().createBranch(PROJECT_NAME, request));
			assertThat(response).as("Created branch").hasName(request.getName());
			if (request.isLatest()) {
				assertThat(response).as("Created branch").isLatest();
			} else {
				assertThat(response).as("Created branch").isNotLatest();
			}
			uuid.append(response.getUuid());
		}, COMPLETED, 1);

		// return new branch
		return tx(() -> project().getBranchRoot().findByUuid(uuid.toString()));
	}

	protected Single<NodeResponse> createBinaryContent() {
		String parentUuid = client().findProjects().blockingGet().getData().get(0).getRootNode().getUuid();
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setParentNodeUuid("uuid");
		request.setSchemaName("binary_content");
		request.setParentNodeUuid(parentUuid);
		return client().createNode(PROJECT_NAME, request).toSingle();
	}

	protected Single<NodeResponse> createBinaryContent(String uuid) {
		String parentUuid = client().findProjects().blockingGet().getData().get(0).getRootNode().getUuid();
		NodeCreateRequest request = new NodeCreateRequest();
		request.setLanguage("en");
		request.setParentNodeUuid("uuid");
		request.setSchemaName("binary_content");
		request.setParentNodeUuid(parentUuid);
		return client().createNode(uuid, PROJECT_NAME, request).toSingle();
	}

	protected Completable stopRestVerticle() {
		return ((BootstrapInitializerImpl) boot()).loader.get().unloadVerticles();
	}

	protected Completable startRestVerticle() {
		return Completable.fromAction(() -> {
			CoreVerticleLoader loader = ((BootstrapInitializerImpl) boot()).loader.get();
			loader.loadVerticles();
			RouterStorage.addProject(TestDataProvider.PROJECT_NAME);
		});
	}

	protected Completable restartRestVerticle() {
		return stopRestVerticle().andThen(startRestVerticle());
	}

	/**
	 * Add an expectation of an event.
	 * 
	 * @param event
	 * @param expectedCount
	 * @param asserter
	 * @return
	 */
	public CompletableFuture<JsonObject> expectEvents(MeshEvent event, int expectedCount, Predicate<JsonObject> asserter) {
		CompletableFuture<JsonObject> fut = new CompletableFuture<>();
		AtomicInteger counter = new AtomicInteger(0);
		vertx().eventBus().consumer(event.getAddress(), (Message<JsonObject> mh) -> {
			counter.incrementAndGet();
			try {
				JsonObject json = mh.body();
				// Only complete the future if the event is accepted.
				if (asserter.test(json)) {
					fut.complete(json);
				}
			} catch (Throwable e) {
				fut.completeExceptionally(e);
			}
		});
		futures.put(fut, event);
		CompletableFuture<Integer> count = CompletableFuture.supplyAsync(() -> {
			return counter.get();
		});
		// futures.put(count, event);
		// TODO assert event count
		return fut;
	}

	/**
	 * Await all futures.
	 * 
	 * @throws Exception
	 */
	public void waitForEvents() throws Exception {
		for (Entry<CompletableFuture<JsonObject>, MeshEvent> entry : futures.entrySet()) {
			MeshEvent event = entry.getValue();
			try {
				entry.getKey().get(1, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new RuntimeException("Did not receive event for {" + event.getAddress() + "}", e);
			}
		}
	}

}
