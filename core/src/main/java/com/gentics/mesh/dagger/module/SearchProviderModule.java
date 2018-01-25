package com.gentics.mesh.dagger.module;

import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.DevNullSearchProvider;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.ElasticSearchProvider;

import dagger.Module;
import dagger.Provides;

@Module
public class SearchProviderModule {

	@Provides
	@Singleton
	public static SearchProvider searchProvider() {
		MeshOptions options = Mesh.mesh().getOptions();
		SearchProvider searchProvider = null;
		// Automatically select the dummy search provider if no directory or
		// options have been specified
		if (options.getSearchOptions().getHosts().isEmpty()) {
			// searchProvider = new TrackingSearchProvider();
			searchProvider = new DevNullSearchProvider();
		} else {
			searchProvider = new ElasticSearchProvider();
		}
		return searchProvider;
	}
}
