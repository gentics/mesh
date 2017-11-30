package com.gentics.mesh.search;

import org.junit.Test;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.json.JsonObject;

public class ElasticSearchProviderTest {

	@Test
	public void testES() {
		ElasticSearchProvider provider = new ElasticSearchProvider();
		MeshOptions options = new MeshOptions();
		options.setNodeName("dummy");
		options.getSearchOptions().setDirectory("target/esprovidertest" + UUIDUtil.randomUUID());
		options.getSearchOptions().setHttpEnabled(true);
		provider.init(options);
		provider.start();
		IndexInfo info = new IndexInfo("test", new JsonObject(), new JsonObject());
		provider.createIndex(info).andThen(provider.deleteIndex("test")).subscribe();
		provider.createIndex(info).andThen(provider.storeDocument("test", "abc", new JsonObject().put("name", "test1234"))).subscribe();
		provider.createIndex(info).andThen(provider.createIndex(info)).subscribe();
	}
}
