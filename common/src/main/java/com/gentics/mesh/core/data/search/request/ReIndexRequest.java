package com.gentics.mesh.core.data.search.request;

import com.gentics.mesh.search.SearchProvider;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * {@link SearchRequest} implementation that will do a reindex from documents of one index into another
 */
public class ReIndexRequest implements SearchRequest {
	private static final Logger log = LoggerFactory.getLogger(ReIndexRequest.class);

	private final String source;

	private final String dest;

	private final JsonObject query;

	/**
	 * Create instance.
	 * @param source source index name
	 * @param dest target index name
	 * @param query restricting query
	 */
	public ReIndexRequest(String source, String dest, JsonObject query) {
		this.source = source;
		this.dest = dest;
		this.query = query;
	}

	@Override
	public int requestCount() {
		return 1;
	}

	@Override
	public Completable execute(SearchProvider searchProvider) {
		return searchProvider.reIndex(source, dest, query).doOnSubscribe(ignore -> {
			if (log.isDebugEnabled()) {
				log.debug("ReIndexing documents from index {" + source + "} to index {" + dest + "}");
			}
		}).doOnComplete(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Done ReIndexing documents from index {" + source + "} to index {" + dest + "}");
			}
		});
	}

	@Override
	public String toString() {
		return String.format("ReIndexRequest(source='%s', dest='%s')", source, dest);
	}
}
