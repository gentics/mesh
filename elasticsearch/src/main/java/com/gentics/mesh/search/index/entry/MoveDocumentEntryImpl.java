package com.gentics.mesh.search.index.entry;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.MOVE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import com.gentics.mesh.core.data.search.MoveDocumentEntry;
import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.context.MoveEntryContext;
import com.gentics.mesh.error.EdgeNotFoundException;
import com.gentics.mesh.error.VertexNotFoundException;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

import io.reactivex.Observable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see MoveDocumentEntry
 */
public class MoveDocumentEntryImpl extends AbstractEntry<MoveEntryContext> implements MoveDocumentEntry {

	private static final Logger log = LoggerFactory.getLogger(MoveDocumentEntryImpl.class);

	private NodeIndexHandler indexHandler;

	public MoveDocumentEntryImpl(NodeIndexHandler indexHandler, MoveEntryContext context) {
		super(MOVE_ACTION);
		this.indexHandler = indexHandler;
		this.context = context;
	}

	@Override
	public Observable<? extends BulkEntry> process() {
		switch (elementAction) {
		case MOVE_ACTION:
			return indexHandler.moveForBulk(this).onErrorResumeNext(err -> {
				if (err instanceof VertexNotFoundException || err instanceof EdgeNotFoundException) {
					log.warn("Graph element could no longer be found. Ignoring the entry.", err);
					return Observable.empty();
				} else {
					return Observable.error(err);
				}
			}).doOnComplete(onProcessAction);
		default:
			throw error(INTERNAL_SERVER_ERROR, "Can't process entry of for action {" + elementAction + "}");
		}

	}
}
