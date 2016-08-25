package com.gentics.mesh.search.impl;

import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.ElasticSearchOptions;
import com.gentics.mesh.search.SearchProvider;

import dagger.Module;
import dagger.Provides;

@Singleton
@Module
public class ElasticSearchProviderModule {

	@Provides
	@Singleton
	public SearchProvider searchProvider() {
		ElasticSearchOptions options = Mesh.mesh().getOptions().getSearchOptions();
		SearchProvider searchProvider = null;
		if (options == null || options.getDirectory() == null) {
			searchProvider = new DummySearchProvider();
		} else {
			searchProvider = new ElasticSearchProvider().init(Mesh.mesh().getOptions().getSearchOptions());
		}
		return searchProvider;
	}

}
