package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.search.context.MoveEntryContext;

/**
 * A mode search queue batch move entry is usually used to move {@link NodeGraphFieldContainer} documents from one index to another. This is needed during the
 * node migration in order to differentially update the indices.
 */
public interface MoveDocumentEntry extends BulkEventQueueEntry<MoveEntryContext> {

}
