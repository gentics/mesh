package com.gentics.mesh.search.index;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.verticle.eventhandler.SyncHandler;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

@Singleton
public class AdminIndexHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminIndexHandler.class);

	private Database db;

	private SearchProvider searchProvider;

	private SyncHandler syncVerticle;

	private IndexHandlerRegistry registry;

	private HandlerUtilities utils;

	@Inject
	public AdminIndexHandler(Database db, SearchProvider searchProvider, SyncHandler syncVerticle, IndexHandlerRegistry registry, HandlerUtilities utils) {
		this.db = db;
		this.searchProvider = searchProvider;
		this.syncVerticle = syncVerticle;
		this.registry = registry;
		this.utils = utils;
	}

	public void handleStatus(InternalActionContext ac) {
		Map<String,Object> metrics = new HashMap<>();
		// TODO fetch state
		// statusResponse.setReindexRunning(REINDEX_FLAG.get());

		// Aggregate all local metrics from all handlers
		for (IndexHandler<?> handler : registry.getHandlers()) {
			String type = handler.getType();
			metrics.put(type, handler.getMetrics());
		}

		searchProvider.isAvailable().map(available ->
			new SearchStatusResponse()
				.setMetrics(metrics)
				.setAvailable(available)
		).subscribe(message -> ac.send(message, OK), ac::fail);
	}

	public void handleSync(InternalActionContext ac) {
		db.asyncTx(() -> Single.just(ac.getUser().hasAdminRole()))
			.subscribe(hasAdminRole -> {
				if (hasAdminRole) {
					SyncHandler.invokeSync();
					ac.send(message(ac, "search_admin_index_sync_invoked"), OK);
				} else {
					ac.fail(error(FORBIDDEN, "error_admin_permission_required"));
				}
			}, ac::fail);
	}

	public void handleClear(InternalActionContext ac) {
		db.asyncTx(() -> Single.just(ac.getUser().hasAdminRole())).flatMapCompletable(hasAdminRole -> {
			if (hasAdminRole) {
				return searchProvider.clear()
					.andThen(Observable.fromIterable(registry.getHandlers())
					.flatMapCompletable(handler -> handler.init()));
			} else {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
		}).subscribe(() -> {
			ac.send(message(ac, "search_admin_index_clear"), OK);
		}, error -> {
			if (error instanceof GenericRestException) {
				ac.fail(error);
			} else {
				log.error("Error while clearing all indices.", error);
				ac.send(message(ac, "search_admin_index_clear_error"), INTERNAL_SERVER_ERROR);
			}
		});
	}

}
