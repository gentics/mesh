package com.gentics.mesh.test.context.helper;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.gentics.mesh.cli.BootstrapInitializerImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.search.verticle.ElasticsearchProcessVerticle;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;
import com.gentics.mesh.test.context.ClientHandler;
import com.gentics.mesh.test.context.event.EventAsserter;
import com.gentics.mesh.test.context.event.EventAsserterChain;
import com.gentics.mesh.test.util.TestUtils;

import io.reactivex.Completable;
import io.reactivex.functions.Action;
import io.vertx.core.eventbus.MessageConsumer;

public interface EventHelper extends BaseHelper {

	EventAsserter eventAsserter();

	/**
	 * Drop all indices and create a new index using the current data.
	 *
	 * @throws Exception
	 */
	default void recreateIndices() throws Exception {
		// We potentially modified existing data thus we need to drop all indices and create them and reindex all data
		SyncEventHandler.invokeClearCompletable(meshApi()).blockingAwait(10, TimeUnit.SECONDS);
		SyncEventHandler.invokeSyncCompletable(meshApi()).blockingAwait(30, TimeUnit.SECONDS);
		refreshIndices();
	}

	/**
	 * Wait until the given event has been received.
	 * 
	 * @param address
	 * @param code
	 * @throws TimeoutException
	 */
	default void waitForEvent(String address, Action code) {
		waitForEvent(address, code, 10_000);
	}

	default void waitForEvent(MeshEvent event, int timeoutMs) {
		waitForEvent(event.getAddress(), () -> {
		}, timeoutMs);
	}

	default void waitForEvent(String address, Action code, int timeoutMs) {
		CountDownLatch latch = new CountDownLatch(1);
		MessageConsumer<Object> consumer = vertx().eventBus().consumer(address);
		consumer.handler(msg -> latch.countDown());
		// The completion handler will be invoked once the consumer has been registered
		consumer.completionHandler(res -> {
			if (res.failed()) {
				throw new RuntimeException("Could not listen to event", res.cause());
			}
			try {
				code.run();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		try {
			latch.await(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		consumer.unregister();
	}

	default void waitForSearchIdleEvent() {
		getTestContext().waitForSearchIdleEvent();
	}

	default void waitForSearchIdleEvent(Completable completable) {
		waitForEvent(MeshEvent.SEARCH_IDLE, () -> {
			completable.subscribe(() -> vertx().eventBus().publish(MeshEvent.SEARCH_FLUSH_REQUEST.address, null));
		});
		refreshIndices();
	}

	default void waitForSearchIdleEvent(Action action) {
		waitForSearchIdleEvent(() -> {
			action.run();
			return null;
		});
	}

	default <T> T waitForSearchIdleEvent(Callable<T> action) {
		try {
			AtomicReference<T> ref = new AtomicReference<>();
			waitForEvent(MeshEvent.SEARCH_IDLE, () -> {
				ref.set(action.call());
				vertx().eventBus().publish(MeshEvent.SEARCH_FLUSH_REQUEST.address, null);
			});
			refreshIndices();
			return ref.get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Wait until the given event has been received.
	 *
	 * @param event
	 * @param code
	 * @throws TimeoutException
	 */
	default void waitForEvent(MeshEvent event, Action code) {
		waitForEvent(event.address, code);
	}

	/**
	 * Wait until the given event has been received.
	 *
	 * @param event
	 * @throws TimeoutException
	 */
	default void waitForEvent(MeshEvent event) {
		waitForEvent(event.address, () -> {
		});
	}

	default void waitForPluginRegistration() {
		waitForEvent(MeshEvent.PLUGIN_REGISTERED, 20_000);
	}

	default JobListResponse waitForJob(Runnable action) {
		return waitForJobs(action, COMPLETED, 1);
	}

	/**
	 * Run the given action with admin permissions enabled.
	 * 
	 * @param action
	 * @throws Exception
	 */
	default <T> T runAsAdmin(Supplier<T> action) {
		boolean isAdmin = tx(() -> user().isAdmin());
		// Grant perms to check the job
		if (!isAdmin) {
			grantAdmin();
		}
		T t = action.get();
		if (!isAdmin) {
			revokeAdmin();
		}
		return t;
	}

	default void runAsAdmin(Runnable action) {
		boolean isAdmin = tx(() -> user().isAdmin());
		// Grant perms to check the job
		if (!isAdmin) {
			grantAdmin();
		}
		action.run();
		if (!isAdmin) {
			revokeAdmin();
		}
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
	default JobListResponse waitForJobs(Runnable action, JobStatus status, int expectedJobs) {

		// Load a status just before the action
		JobListResponse before = runAsAdmin(() -> {
			return call(() -> client().findJobs());
		});

		// Invoke the action
		action.run();

		// Now poll the migration status and check the response
		final int MAX_WAIT = 30;
		JobListResponse response;
		for (int i = 0; i < MAX_WAIT; i++) {

			response = runAsAdmin(() -> call(() -> client().findJobs()));

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
			if (i == MAX_WAIT - 1) {
				String json = response == null ? "NULL" : response.toJson();
				throw new RuntimeException("Migration did not complete within " + MAX_WAIT + " seconds. Last job response was:\n" + json);
			}
			sleep(1000);
		}
		return null;
	}

	default void waitForLatestJob(Runnable action) {
		waitForLatestJob(action, JobStatus.COMPLETED);
	}

	default void waitForLatestJob(Runnable action, JobStatus status) {
		// Load a status just before the action
		JobListResponse before = runAsAdmin(() -> call(() -> client().findJobs()));

		// Invoke the action
		action.run();

		// Now poll the migration status and check the response
		final int MAX_WAIT = 120;
		for (int i = 0; i < MAX_WAIT; i++) {
			JobListResponse response = runAsAdmin(() -> call(() -> client().findJobs()));
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
	default JobResponse waitForJob(Runnable action, String jobUuid, JobStatus status) {
		// Invoke the action
		action.run();

		// Now poll the migration status and check the response
		final int MAX_WAIT = 120;
		for (int i = 0; i < MAX_WAIT; i++) {
			JobResponse response = runAsAdmin(() -> call(() -> client().findJobByUuid(jobUuid)));

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
	default JobListResponse triggerAndWaitForJob(String jobUuid) {
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
	default JobListResponse triggerAndWaitForJob(String jobUuid, JobStatus status) {
		waitForJob(() -> {
			MeshEvent.triggerJobWorker(meshApi());
		}, jobUuid, status);
		return runAsAdmin(() -> call(() -> client().findJobs()));
	}

	default void triggerAndWaitForAllJobs(JobStatus expectedStatus) {
		MeshEvent.triggerJobWorker(meshApi());

		// Now poll the migration status and check the response
		final int MAX_WAIT = 120;
		for (int i = 0; i < MAX_WAIT; i++) {
			JobListResponse response = runAsAdmin(() -> call(() -> client().findJobs(new PagingParametersImpl().setPerPage(200L))));

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
	 * Call the given handler, latch for the future and assert success. Waits for search to be idle, then returns the result.
	 *
	 * @param handler
	 *            handler
	 * @param <T>
	 *            type of the returned object
	 * @return result of the future
	 */
	default <T> T callAndWait(ClientHandler<T> handler) {
		try {
			return waitForSearchIdleEvent(() -> handler.handle().blockingGet());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	default void refreshIndices() {
		getSearchVerticle().refresh().blockingAwait(15, TimeUnit.SECONDS);
	}

	default ElasticsearchProcessVerticle getSearchVerticle() {
		return ((BootstrapInitializerImpl) boot()).loader.get().getSearchVerticle();
	}

	/**
	 * Return the event asserter.
	 *
	 * @return
	 */
	default EventAsserterChain expect(MeshEvent event) {
		return eventAsserter().expect(event);
	}

	default void awaitEvents() {
		eventAsserter().await();
	}

}
