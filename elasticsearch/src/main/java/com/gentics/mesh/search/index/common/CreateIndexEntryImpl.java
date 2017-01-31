package com.gentics.mesh.search.index.common;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_INDEX;

import javax.inject.Inject;

import com.gentics.mesh.core.data.HandleContext;
import com.gentics.mesh.core.data.search.CreateIndexEntry;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;

import rx.Completable;

/**
 * @see CreateIndexEntry
 */
public class CreateIndexEntryImpl implements CreateIndexEntry {

	private String indexName;
	private String indexType;
	private IndexHandler indexHandler;
	private HandleContext context = new HandleContext();

	@Inject
	public CreateIndexEntryImpl(IndexHandler indexHandler, String indexName, String indexType) {
		this.indexName = indexName;
		this.indexType = indexType;
		this.indexHandler = indexHandler;
	}

	@Override
	public String getIndexName() {
		return this.indexName;
	}

	@Override
	public String getIndexType() {
		return this.indexType;
	}

	@Override
	public SearchQueueEntryAction getElementAction() {
		return CREATE_INDEX;
	}

	@Override
	public Completable process() {
		return indexHandler.createIndex(this);
	}

	@Override
	public HandleContext getContext() {
		return context;
	}

	@Override
	public String toString() {
		return "Create Index Entry - indexName: " + getIndexName() + " type: " + getIndexType() + " handler: "
				+ indexHandler.getClass().getSimpleName();
	}

}
