package com.gentics.mesh.core.data.search;

import com.gentics.mesh.core.data.search.context.EntryContext;
import io.reactivex.Completable;

/**
 * Dedicated search queue entry which is not bulkable or which must not be included in a bulk. (e.g. index create, index drop).
 * 
 * @param <T>
 */
public interface SeperateSearchQueueEntry<T extends EntryContext> extends SearchQueueEntry<T> {

	/**
	 * Process the entry.
	 *
	 * @return
	 */
	Completable process();
}
