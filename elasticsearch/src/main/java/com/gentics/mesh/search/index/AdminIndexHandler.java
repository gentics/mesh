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

@Singleton
public class AdminIndexHandler {

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

	public void handleReindex(InternalActionContext ac) {
		db.asyncTx(() -> {
			if (ac.getUser().hasAdminRole()) {
				return searchProvider.clear().andThen(Observable.fromIterable(registry.getHandlers())

						.flatMapCompletable(handler -> handler.init().andThen(handler.reindexAll()))

						.andThen(searchProvider.refreshIndex()).andThen(Single.just(message(ac, "search_admin_reindex_invoked"))));
			} else {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
		}).subscribe(message -> ac.send(message, OK), ac::fail);
	}

}
