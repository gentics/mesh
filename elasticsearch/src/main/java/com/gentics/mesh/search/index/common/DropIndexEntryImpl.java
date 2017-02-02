package com.gentics.mesh.search.index.common;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DROP_INDEX;

import javax.inject.Inject;

import com.gentics.mesh.core.data.search.DropIndexEntry;
import com.gentics.mesh.search.index.entry.AbstractEntry;

import rx.Completable;

/**
 * @see DropIndexEntry
 */
public class DropIndexEntryImpl extends AbstractEntry implements DropIndexEntry {

	private String indexName;

	private DropIndexHandler handler;

	@Inject
	public DropIndexEntryImpl(DropIndexHandler handler, String indexName) {
		super(DROP_INDEX);
		this.handler = handler;
		this.indexName = indexName;
	}

	@Override
	public String getIndexName() {
		return this.indexName;
	}

	@Override
	public String getIndexType() {
		return null;
	}

	@Override
	public Completable process() {
		return handler.dropIndex(this);
	}

	@Override
	public String toString() {
		return "Drop Entry - indexName: " + getIndexName() + " for handler: " + handler.getClass().getSimpleName();
	}

}
