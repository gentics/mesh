package com.gentics.mesh.test.context;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.cli.BootstrapInitializerImpl;
import com.gentics.mesh.cli.CoreVerticleLoader;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.router.ProjectsRouter;
import com.gentics.mesh.test.TestDataProvider;
import com.gentics.mesh.test.context.event.EventAsserter;
import com.gentics.mesh.test.docker.ElasticsearchContainer;
import com.gentics.mesh.test.util.MeshAssert;

import eu.rekawek.toxiproxy.model.ToxicList;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.RoutingContext;
import okhttp3.OkHttpClient;

public abstract class AbstractMeshTest implements TestHttpMethods, TestGraphHelper, PluginHelper {

	static {
		// Use slf4j instead of JUL
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	private OkHttpClient httpClient;

	private EventAsserter eventAsserter = new EventAsserter(testContext);

	@Rule
	@ClassRule
	public static MeshTestContext testContext = new MeshTestContext();

	@Override
	public MeshTestContext getTestContext() {
		return testContext;
	}

	@After
	public void clearLatches() {
		eventAsserter().clear();
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
			int timeout;
			try {
				timeout = MeshAssert.getTimeout();
				this.httpClient = new OkHttpClient.Builder()
					.writeTimeout(timeout, TimeUnit.SECONDS)
					.readTimeout(timeout, TimeUnit.SECONDS)
					.connectTimeout(timeout, TimeUnit.SECONDS)
					.build();
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
		}
		return this.httpClient;
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

	/**
	 * Return toxics for ES proxy.
	 *
	 * @return
	 */
	public ToxicList toxics() {
		return MeshTestContext.getProxy().toxics();
	}

	/**
	 * Return the used elasticsearch container.
	 *
	 * @return
	 */
	public ElasticsearchContainer elasticsearch() {
		return MeshTestContext.elasticsearchContainer();
	}

	@Override
	public EventAsserter eventAsserter() {
		return eventAsserter;
	}
}
