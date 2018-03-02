package com.gentics.mesh.search.index;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class AdminIndexHandler {
	private static final Logger log = LoggerFactory.getLogger(AdminIndexHandler.class);

	private IndexHandlerRegistry registry;

	private Database db;

	private SearchProvider searchProvider;

	@Inject
	public AdminIndexHandler(Database db, SearchProvider searchProvider, IndexHandlerRegistry registry) {
		this.db = db;
		this.searchProvider = searchProvider;
		this.registry = registry;
	}

	public void handleStatus(InternalActionContext ac) {
		db.tx(() -> {
			SearchStatusResponse statusResponse = new SearchStatusResponse();
			return Observable.just(statusResponse);
		}).subscribe(message -> ac.send(message, OK), ac::fail);
	}

	private void doReindex() {
		searchProvider.clear().andThen(Observable.fromIterable(registry.getHandlers())
				.flatMapCompletable(handler -> handler.init().andThen(handler.reindexAll()))
				.andThen(searchProvider.refreshIndex()))
				.subscribe(() -> log.info("Reindex complete"), log::error);
	}

	public void handleReindex(InternalActionContext ac) {
		db.asyncTx(() -> Single.just(ac.getUser().hasAdminRole()))
			.subscribe(hasAdminRole -> {
				if (hasAdminRole) {
					ac.send(message(ac, "search_admin_reindex_invoked"), OK);
					doReindex();
				} else {
					ac.fail(error(FORBIDDEN, "error_admin_permission_required"));
				}
			}, ac::fail);
	}

}
