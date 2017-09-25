package com.gentics.mesh.search.index.entry;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.MOVE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import com.gentics.mesh.core.data.search.MoveDocumentEntry;
import com.gentics.mesh.core.data.search.context.MoveEntryContext;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

import rx.Completable;

/**
 * @see MoveDocumentEntry
 */
public class MoveDocumentEntryImpl extends AbstractEntry<MoveEntryContext> implements MoveDocumentEntry {

	private NodeIndexHandler indexHandler;

	public MoveDocumentEntryImpl(NodeIndexHandler indexHandler, MoveEntryContext context) {
		super(MOVE_ACTION);
		this.indexHandler = indexHandler;
		this.context = context;
	}

	@Override
	public Completable process() {
		switch (elementAction) {
		case MOVE_ACTION:
			return indexHandler.move(this);
		default:
			throw error(INTERNAL_SERVER_ERROR, "Can't process entry of for action {" + elementAction + "}");
		}
	}

}
