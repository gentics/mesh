package com.gentics.mesh.search.index.common;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.search.DropIndexEntry;
import com.gentics.mesh.search.SearchProvider;

import io.reactivex.Completable;

/**
 * @see DropIndexHandler
 */
@Singleton
public class DropIndexHandlerImpl implements DropIndexHandler {

	private SearchProvider searchProvider;

	@Inject
	public DropIndexHandlerImpl(SearchProvider searchProvider) {
		this.searchProvider = searchProvider;
	}

	@Override
	public Completable dropIndex(DropIndexEntry entry) {
		String indexName = entry.getIndexName();
		return searchProvider.deleteIndex(indexName);
	}

	protected org.elasticsearch.node.Node getESNode() {
		// Fetch the elastic search instance
		if (searchProvider.getNode() != null && searchProvider.getNode() instanceof org.elasticsearch.node.Node) {
			return (org.elasticsearch.node.Node) searchProvider.getNode();
		} else {
			throw new RuntimeException("Unable to get elasticsearch instance from search provider got {" + searchProvider.getNode() + "}");
		}
	}

}
