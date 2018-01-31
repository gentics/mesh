package com.gentics.mesh.search;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class ElasticSearchProviderTest extends AbstractMeshTest {

	@Test
	public void testProvider() throws IOException {
		ElasticSearchProvider provider = ((ElasticSearchProvider) searchProvider());
		provider.createIndex(new IndexInfo("test", new JsonObject(), new JsonObject())).blockingAwait();
		String uuid = UUIDUtil.randomUUID();
		// provider.storeDocument("test", uuid, new JsonObject()).blockingAwait();
		provider.updateDocument("test", uuid, new JsonObject(), true).blockingAwait();

		provider.deleteDocument("test", uuid).blockingAwait();

		// Should not fail if the document is gone since we end result would be the same.
		provider.deleteDocument("test", uuid).blockingAwait();

		provider.deleteIndex("testindex").blockingAwait();

		provider.createIndex(new IndexInfo("testindex", new JsonObject(), new JsonObject())).blockingAwait();

		provider.createIndex(new IndexInfo("testindex", new JsonObject(), new JsonObject())).blockingAwait();

		provider.deleteIndex("testindex").blockingAwait();

		// provider.validateCreateViaTemplate(new IndexInfo("test", new JsonObject(), new JsonObject())).blockingAwait();

	}

	@Test
	public void testConcurrencyConflictError() {
		ElasticSearchProvider provider = ((ElasticSearchProvider) searchProvider());
		provider.createIndex(new IndexInfo("test", new JsonObject(), new JsonObject())).blockingAwait();
		provider.storeDocument("test", "1", new JsonObject().put("value", 0)).blockingAwait();
		Observable.range(1, 2000).flatMapCompletable(i -> provider.updateDocument("test", "1", new JsonObject().put("value", i), false))
			.blockingAwait();
	}
}
