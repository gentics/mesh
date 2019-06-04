package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DropIndexRequest implements SearchRequest {

	private static final Logger log = LoggerFactory.getLogger(CreateIndexRequest.class);
	private String indexName;

	public DropIndexRequest(String indexName) {
		this.indexName = indexName;
	}

	@Override
	public int requestCount() {
		return 1;
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.deleteIndex(false, indexName)
			.doOnSubscribe(ignore -> {
				if (log.isDebugEnabled()) {
					log.debug("Deleting index {" + indexName + "}");
				}
			});
	}

	@Override
	public String toString() {
		return indexName;
	}

}