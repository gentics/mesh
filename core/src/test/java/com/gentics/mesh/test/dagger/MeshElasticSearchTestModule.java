package com.gentics.mesh.test.dagger;

import com.gentics.mesh.etc.ElasticSearchOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.ElasticSearchProvider;

import dagger.Module;
import dagger.Provides;


@Module
public class MeshElasticSearchTestModule extends MeshTestModule {

	@Provides
	public SearchProvider testSearchProvider() {
		ElasticSearchOptions options = new ElasticSearchOptions();
		options.setDirectory("target/elasticsearch_data_" + System.currentTimeMillis());
//		options.setHttpEnabled(true);
		SearchProvider provider = new ElasticSearchProvider().init(options);
		return provider;
	}

}
