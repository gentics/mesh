package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.search.context.EntryContext;
import io.reactivex.Completable;

public interface SeperateSearchQueueEntry<T extends EntryContext> extends SearchQueueEntry<T> {
	/**
	 * Process the entry.
	 *
	 * @return
	 */
	Completable process();
}
