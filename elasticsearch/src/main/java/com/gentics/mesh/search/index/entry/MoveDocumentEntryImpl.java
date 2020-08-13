package com.gentics.mesh.search.index.entry;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.MOVE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import com.gentics.mesh.core.data.search.MoveDocumentEntry;
import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.context.MoveEntryContext;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;

import io.reactivex.Observable;

/**
 * @see MoveDocumentEntry
 */
public class MoveDocumentEntryImpl extends AbstractEntry<MoveEntryContext> implements MoveDocumentEntry {

	private NodeIndexHandlerImpl indexHandler;

	public MoveDocumentEntryImpl(NodeIndexHandlerImpl indexHandler, MoveEntryContext context) {
		super(MOVE_ACTION);
		this.indexHandler = indexHandler;
		this.context = context;
	}

	@Override
	public Observable<? extends BulkEntry> process() {
		switch (elementAction) {
		case MOVE_ACTION:
			return indexHandler.moveForBulk(this).doOnComplete(onProcessAction);
		default:
			throw error(INTERNAL_SERVER_ERROR, "Can't process entry of for action {" + elementAction + "}");
		}

	}
}
