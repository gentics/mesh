package com.gentics.mesh.test.context;

import static com.gentics.mesh.Events.JOB_WORKER_ADDRESS;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.util.TestUtils.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheckHandler;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.GroupCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.MicroschemaContainerCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.NodeCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.ProjectCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.ReleaseCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.RoleCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.SchemaContainerCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.TagCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.TagFamilyCheck;
import com.gentics.mesh.core.verticle.admin.consistency.asserter.UserCheck;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.test.TestDataProvider;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;
import io.vertx.ext.web.RoutingContext;
import rx.functions.Action0;

public abstract class AbstractMeshTest implements TestHelperMethods {

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	@Rule
	@ClassRule
	public static MeshTestContext testContext = new MeshTestContext();

	@Override
	public MeshTestContext getTestContext() {
		return testContext;
	}

	@After
	public void checkConsistency() {
		try (Tx tx = tx()) {
			ConsistencyCheckResponse response = new ConsistencyCheckResponse();
			for (ConsistencyCheck check : ConsistencyCheckHandler.getChecks()) {
				check.invoke(db(), response);
			}

			assertThat(response.getInconsistencies()).as("Inconsistencies").isEmpty();
		}
	}

	/**
	 * Drop all indices and create a new index using the current data.
	 * 
	 * @throws Exception
	 */
	protected void recreateIndices() throws Exception {
		// We potentially modified existing data thus we need to drop all indices and create them and reindex all data
		MeshInternal.get().searchProvider().clear();
		for (IndexHandler<?> handler : MeshInternal.get().indexHandlerRegistry().getHandlers()) {
			handler.init().await();
			handler.reindexAll().await();
		}
	}

	public String getJson(Node node) throws Exception {
		InternalActionContext ac = mockActionContext("lang=en&version=draft");
		ac.data().put(RouterStorage.PROJECT_CONTEXT_KEY, TestDataProvider.PROJECT_NAME);
		return JsonUtil.toJson(node.transformToRest(ac, 0).toBlocking().value());
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
	protected JobListResponse waitForJobs(Action0 action, MigrationStatus status, int expectedJobs) {
		// Load a status just before the action
		JobListResponse before = call(() -> client().findJobs());

		// Invoke the action
		action.call();

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
			if (i == MAX_WAIT) {
				throw new RuntimeException("Migration did not complete within " + MAX_WAIT + " seconds");
			}
			sleep(1000);
		}
		return null;
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
	protected JobResponse waitForJob(Action0 action, String jobUuid, MigrationStatus status) {
		// Invoke the action
		action.call();

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

			if (i == MAX_WAIT) {
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
			vertx().eventBus().send(JOB_WORKER_ADDRESS, null);
		}, jobUuid, status);
		return call(() -> client().findJobs());
	}

	protected void triggerAndWaitForAllJobs(MigrationStatus expectedStatus) {
		vertx().eventBus().send(JOB_WORKER_ADDRESS, null);

		// Now poll the migration status and check the response
		final int MAX_WAIT = 120;
		for (int i = 0; i < MAX_WAIT; i++) {
			JobListResponse response = call(() -> client().findJobs(new PagingParametersImpl().setPerPage(200)));

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

			if (i == MAX_WAIT) {
				throw new RuntimeException("Job did not complete within " + MAX_WAIT + " seconds");
			}
			sleep(1000);
		}

	}

}
