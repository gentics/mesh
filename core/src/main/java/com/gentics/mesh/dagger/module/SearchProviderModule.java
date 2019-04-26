package com.gentics.mesh.dagger.module;

import com.gentics.mesh.dagger.SearchProviderType;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.DevNullSearchProvider;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.search.impl.SearchClient;
import dagger.Module;
import dagger.Provides;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.net.MalformedURLException;
import java.net.URL;

@Module
public class SearchProviderModule {

	@Provides
	@Singleton
	public static SearchProvider searchProvider(@Nullable SearchProviderType type, MeshOptions options, ElasticSearchProvider elasticsearchProvider) {
		if (type == null) {
			if (options.getSearchOptions().getUrl() == null) {
				return new DevNullSearchProvider();
			} else {
				return elasticsearchProvider;
			}
		}
		switch (type) {
			case NULL:
				return new DevNullSearchProvider();
			case TRACKING:
				return new TrackingSearchProvider();
			case ELASTICSEARCH:
			default:
				return elasticsearchProvider;
		}
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
