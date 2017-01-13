package com.gentics.mesh.search.index.common;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;

import com.gentics.mesh.core.data.search.DropIndexEntry;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;

@Singleton
public class CommonIndexHandlerImpl implements DropIndexHandler {

	private static final Logger log = LoggerFactory.getLogger(CommonIndexHandlerImpl.class);

	SearchProvider searchProvider;

	@Inject
	public CommonIndexHandlerImpl(SearchProvider searchProvider) {
		this.searchProvider = searchProvider;
	}

	@Override
	public Completable dropIndex(DropIndexEntry entry) {
		String indexName = entry.getIndexName();
		if (searchProvider.getNode() != null) {
			return Completable.create(sub -> {
				org.elasticsearch.node.Node node = getESNode();
				DeleteIndexRequestBuilder request = node.client().admin().indices().prepareDelete(indexName);
				request.execute(new ActionListener<DeleteIndexResponse>() {

					@Override
					public void onResponse(DeleteIndexResponse response) {
						if (log.isDebugEnabled()) {
							log.debug("Deleted index {" + indexName + "}");
						}
						sub.onCompleted();
					}

					@Override
					public void onFailure(Throwable e) {
						sub.onError(e);
					}
				});

			});

		} else {
			return Completable.complete();
		}
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
