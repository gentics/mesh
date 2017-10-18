package com.gentics.mesh.search;

import org.junit.Test;

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
		provider.createIndex("test").andThen(provider.deleteIndex("test")).subscribe();
		provider.createIndex("test").andThen(provider.storeDocument("test", "test", "abc", new JsonObject().put("name", "test1234"))).subscribe();
		provider.createIndex("test").andThen(provider.createIndex("test")).subscribe();
	}
}
