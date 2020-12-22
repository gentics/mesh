package com.gentics.mesh.search.index;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.search.EntityMetrics;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistryImpl;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for index admin operations (purge,sync)
 */
@Singleton
public class AdminIndexHandler {

	private static final Logger log = LoggerFactory.getLogger(AdminIndexHandler.class);

	private final Vertx vertx;

	private Database db;

	private SearchProvider searchProvider;

	private SyncEventHandler syncVerticle;

	private IndexHandlerRegistryImpl registry;

	private HandlerUtilities utils;

	@Inject
	public AdminIndexHandler(Vertx vertx, Database db, SearchProvider searchProvider, SyncEventHandler syncVerticle,
		IndexHandlerRegistryImpl registry, HandlerUtilities utils) {
		this.vertx = vertx;
		this.db = db;
		this.searchProvider = searchProvider;
		this.syncVerticle = syncVerticle;
		this.registry = registry;
		this.utils = utils;
	}

	/**
	 * Return the search status to the client.
	 * 
	 * @param ac
	 */
	public void handleStatus(InternalActionContext ac) {
		getSearchStatus().subscribe(message -> ac.send(message, OK), ac::fail);
	}

	/**
	 * Return the search status.
	 * 
	 * @return
	 */
	public Single<SearchStatusResponse> getSearchStatus() {
		Map<String, EntityMetrics> metrics = new HashMap<>();
		// TODO fetch state
		// statusResponse.setReindexRunning(REINDEX_FLAG.get());

		// Aggregate all local metrics from all handlers
		for (IndexHandler<?> handler : registry.getHandlers()) {
			String type = handler.getType();
			metrics.put(type, handler.getMetrics());
		}

		return searchProvider.isAvailable().map(available -> new SearchStatusResponse()
			.setMetrics(metrics)
			.setAvailable(available));
	}

	/**
	 * Invoke the search index sync.
	 * 
	 * @param ac
	 */
	public void handleSync(InternalActionContext ac) {
		db.asyncTx(() -> Single.just(ac.getUser().isAdmin()))
			.subscribe(isAdmin -> {
				if (isAdmin) {
					SyncEventHandler.invokeSync(vertx);
					ac.send(message(ac, "search_admin_index_sync_invoked"), OK);
				} else {
					ac.fail(error(FORBIDDEN, "error_admin_permission_required"));
				}
			}, ac::fail);
	}

	/**
	 * Invoke the search index clear and init handlers to re-create the search indices.
	 * 
	 * @param ac
	 */
	public void handleClear(InternalActionContext ac) {
		db.asyncTx(() -> Single.just(ac.getUser().isAdmin())).flatMapCompletable(isAdmin -> {
			if (isAdmin) {
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
