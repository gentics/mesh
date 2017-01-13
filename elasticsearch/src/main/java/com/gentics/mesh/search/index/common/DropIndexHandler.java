package com.gentics.mesh.search.index.common;

import com.gentics.mesh.core.data.search.DropIndexEntry;

import rx.Completable;

/**
 * The drop index handler is used to handle {@link DropIndexEntry} batch entries.
 */
public interface DropIndexHandler {

	/**
	 * Invoke a drop index using the provided information.
	 * 
	 * @param entry
	 * @return
	 */
	Completable dropIndex(DropIndexEntry entry);

}
