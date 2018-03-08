package com.gentics.mesh.search.index;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Events;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class AdminIndexHandler {
	private static final Logger log = LoggerFactory.getLogger(AdminIndexHandler.class);

	private Database db;

	private SearchProvider searchProvider;

	private static final String REINDEX_LOCK = "MESH_REINDEX_LOCK";

	// TODO better use a shared data entry to check the cluster-wide status
	private static AtomicBoolean REINDEX_FLAG = new AtomicBoolean(false);

	@Inject
	public AdminIndexHandler(Database db, SearchProvider searchProvider) {
		this.db = db;
		this.searchProvider = searchProvider;
	}

	public void handleStatus(InternalActionContext ac) {
		db.tx(() -> {
			SearchStatusResponse statusResponse = new SearchStatusResponse();
			statusResponse.setReindexRunning(REINDEX_FLAG.get());
			return Observable.just(statusResponse);
		}).subscribe(message -> ac.send(message, OK), ac::fail);
	}

	private void doReindex(InternalActionContext ac) {
		Mesh.rxVertx().sharedData().rxGetLockWithTimeout(REINDEX_LOCK, 2000).doOnError(error -> {
			ac.send(message(ac, "search_admin_reindex_already_in_progress"), SERVICE_UNAVAILABLE);
		}).flatMapCompletable(lock -> {
			ac.send(message(ac, "search_admin_reindex_invoked"), OK);
			REINDEX_FLAG.set(true);
			return searchProvider.invokeReindex()
				.doAfterTerminate(lock::release)
				.doOnDispose(lock::release);
		}).subscribe(() -> {
			REINDEX_FLAG.set(false);
			Mesh.vertx().eventBus().publish(Events.EVENT_REINDEX_COMPLETED, null);
			log.info("Reindex complete");
		}, error -> {
			REINDEX_FLAG.set(false);
			Mesh.vertx().eventBus().publish(Events.EVENT_REINDEX_FAILED, null);
			log.error(error);
		});

	}

	public void handleReindex(InternalActionContext ac) {
		db.asyncTx(() -> Single.just(ac.getUser().hasAdminRole()))
			.subscribe(hasAdminRole -> {
				if (hasAdminRole) {
					doReindex(ac);
				} else {
					ac.fail(error(FORBIDDEN, "error_admin_permission_required"));
				}
			}, ac::fail);
	}

}
