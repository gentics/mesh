package com.gentics.mesh.search.index.common;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_INDEX;

import javax.inject.Inject;

import com.gentics.mesh.core.data.search.CreateIndexEntry;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.search.context.GenericEntryContext;
import com.gentics.mesh.core.data.search.context.impl.GenericEntryContextImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.search.index.entry.AbstractEntry;

import io.reactivex.Completable;

/**
 * @see CreateIndexEntry
 */
public class CreateIndexEntryImpl extends AbstractEntry<GenericEntryContext> implements CreateIndexEntry {

	private String indexName;
	private Schema schema;
	private IndexHandler<?> indexHandler;
	private GenericEntryContextImpl context = new GenericEntryContextImpl();

	@Inject
	public CreateIndexEntryImpl(IndexHandler<?> indexHandler, String indexName) {
		super(CREATE_INDEX);
		this.indexName = indexName;
		this.indexHandler = indexHandler;
	}

	@Override
	public String getIndexName() {
		return this.indexName;
	}

	@Override
	public Schema getSchema() {
		return schema;
	}

	@Override
	public CreateIndexEntry setSchema(Schema schema) {
		this.schema = schema;
		return this;
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
	public GenericEntryContextImpl getContext() {
		return context;
	}

	@Override
	public String toString() {
		return "Create Index Entry - indexName: " + getIndexName() + " handler: " + indexHandler.getClass().getSimpleName();
	}

}
