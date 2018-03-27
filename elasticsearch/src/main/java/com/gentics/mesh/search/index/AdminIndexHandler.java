package com.gentics.mesh.search.index;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Events;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.verticle.ElasticsearchSyncVerticle;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class AdminIndexHandler {
	private static final Logger log = LoggerFactory.getLogger(AdminIndexHandler.class);

	private Database db;

	private SearchProvider searchProvider;

	private ElasticsearchSyncVerticle syncVerticle;

	private IndexHandlerRegistry registry;

	@Inject
	public AdminIndexHandler(Database db, SearchProvider searchProvider, ElasticsearchSyncVerticle syncVerticle, IndexHandlerRegistry registry) {
		this.db = db;
		this.searchProvider = searchProvider;
		this.syncVerticle = syncVerticle;
		this.registry = registry;
	}

	public void handleStatus(InternalActionContext ac) {
		db.tx(() -> {
			SearchStatusResponse statusResponse = new SearchStatusResponse();
			// TODO fetch state
			// statusResponse.setReindexRunning(REINDEX_FLAG.get());

			// Aggregate all local metrics from all handlers
			for (IndexHandler<?> handler : registry.getHandlers()) {
				String type = handler.getType();
				statusResponse.getMetrics().put(type, handler.getMetrics());
			}

			return Observable.just(statusResponse);
		}).subscribe(message -> ac.send(message, OK), ac::fail);
	}

	private void triggerSync(InternalActionContext ac) {

		Mesh.mesh().getVertx().eventBus().send(Events.INDEX_SYNC_WORKER_ADDRESS, null);
		// return searchProvider.in.invokeReindex();
		// .doAfterTerminate(lock::release)
		// .doOnDispose(lock::release);

		// Mesh.rxVertx().sharedData().rxGetLockWithTimeout(REINDEX_LOCK, 2000).doOnError(error -> {
		// ac.send(message(ac, "search_admin_reindex_already_in_progress"), SERVICE_UNAVAILABLE);
		// }).flatMapCompletable(lock -> {
		// ac.send(message(ac, "search_admin_reindex_invoked"), OK);
		// REINDEX_FLAG.set(true);
		// return searchProvider.invokeReindex()
		// .doAfterTerminate(lock::release)
		// .doOnDispose(lock::release);
		// }).subscribe(() -> {
		// REINDEX_FLAG.set(false);
		// Mesh.vertx().eventBus().publish(Events.EVENT_REINDEX_COMPLETED, null);
		// log.info("Reindex complete");
		// }, error -> {
		// REINDEX_FLAG.set(false);
		// Mesh.vertx().eventBus().publish(Events.EVENT_REINDEX_FAILED, null);
		// log.error(error);
		// });

	}

	public void handleSync(InternalActionContext ac) {
		db.asyncTx(() -> Single.just(ac.getUser().hasAdminRole()))
			.subscribe(hasAdminRole -> {
				if (hasAdminRole) {
					triggerSync(ac);
				} else {
					ac.fail(error(FORBIDDEN, "error_admin_permission_required"));
				}
			}, ac::fail);
	}

}
