package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.search.SearchProvider;
import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class CreateIndexRequest implements SearchRequest {

	private static final Logger log = LoggerFactory.getLogger(CreateIndexRequest.class);

	private final IndexInfo indexInfo;

	public CreateIndexRequest(IndexInfo indexInfo) {
		this.indexInfo = indexInfo;
	}

	@Override
	public int requestCount() {
		return 1;
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.createIndex(indexInfo)
			.doOnSubscribe(ignore -> {
				if (log.isDebugEnabled()) {
					log.debug("Creating index {" + indexInfo + "}");
				}
			});
	}

	@Override
	public String toString() {
		return "CreateIndexRequest{" +
			"indexInfo=" + indexInfo +
			'}';
	}
}
