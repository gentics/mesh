package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.context.EntryContext;
import io.reactivex.Observable;

public interface BulkEventQueueEntry<T extends EntryContext> extends SearchQueueEntry<T> {
	/**
	 * Process the entry and generate bulk entries.
	 *
	 * @return
	 */
	Observable<? extends BulkEntry> process();
}
