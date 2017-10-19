package com.gentics.mesh.search.index.common;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;

import com.gentics.mesh.core.data.search.DropIndexEntry;
import com.gentics.mesh.search.SearchProvider;

import rx.Completable;

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
	
//	protected Client getESNode() {
//		return searchProvider.getClient();
//	}

}
