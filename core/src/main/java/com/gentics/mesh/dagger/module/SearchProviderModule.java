package com.gentics.mesh.dagger.module;

import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.DevNullSearchProvider;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.impl.ElasticSearchProvider;

import com.gentics.mesh.search.impl.SearchClient;
import dagger.Module;
import dagger.Provides;

import java.net.MalformedURLException;
import java.net.URL;

@Module
public class SearchProviderModule {

	@Provides
	@Singleton
	public static SearchProvider searchProvider(ElasticSearchProvider elasticsearchProvider) {
		MeshOptions options = Mesh.mesh().getOptions();
		SearchProvider searchProvider = null;
		// Automatically select the dummy search provider if no directory or
		// options have been specified
		String scope = System.getProperty(TrackingSearchProvider.TEST_PROPERTY_KEY);
		if (options.getSearchOptions().getUrl() == null) {
			searchProvider = new DevNullSearchProvider();
		} else if (scope != null) {
			searchProvider = new TrackingSearchProvider();
		} else {
			searchProvider = elasticsearchProvider;
		}
		return searchProvider;
	}

	@Provides
	public static SearchClient searchClientProvider(MeshOptions options) {
		URL url;
		try {
			url = new URL(options.getSearchOptions().getUrl());
		} catch (MalformedURLException e) {
			throw new RuntimeException("Invalid search provider url", e);
		}
		int port = url.getPort();
		String proto = url.getProtocol();
		if ("http".equals(proto) && port == -1) {
			port = 80;
		}
		if ("https".equals(proto) && port == -1) {
			port = 443;
		}
		return new SearchClient(proto, url.getHost(), port);
	}

}
