package com.gentics.mesh.dagger.module;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import javax.annotation.Nullable;
import javax.inject.Singleton;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.okhttp.ElasticsearchOkClient;
import com.gentics.elasticsearch.client.okhttp.ElasticsearchOkClient.Builder;
import com.gentics.mesh.dagger.SearchProviderType;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.search.DevNullSearchProvider;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.impl.ElasticSearchProvider;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.json.JsonObject;

@Module
public class SearchProviderModule {

	@Provides
	@Singleton
	public static SearchProvider searchProvider(@Nullable SearchProviderType type, MeshOptions options, Lazy<ElasticSearchProvider> elasticsearchProvider) {
		if (type == null) {
			if (options.getSearchOptions().getUrl() == null) {
				return new DevNullSearchProvider();
			} else {
				return elasticsearchProvider.get();
			}
		}
		switch (type) {
		case NULL:
			return new DevNullSearchProvider();
		case TRACKING:
			return new TrackingSearchProvider();
		case ELASTICSEARCH:
		default:
			return elasticsearchProvider.get();
		}
	}

	@Provides
	@Singleton
	public static ElasticsearchClient<JsonObject> searchClient(MeshOptions options) {

		ElasticSearchOptions searchOptions = options.getSearchOptions();
		URL url;
		try {
			url = new URL(searchOptions.getUrl());
		} catch (MalformedURLException e) {
			throw new RuntimeException("Invalid search provider url {" + searchOptions.getUrl() + "}", e);
		}
		int port = url.getPort();
		String proto = url.getProtocol();
		if ("http".equals(proto) && port == -1) {
			port = 80;
		}
		if ("https".equals(proto) && port == -1) {
			port = 443;
		}

		Builder<JsonObject> builder = new ElasticsearchOkClient.Builder<JsonObject>()
			.setScheme(proto)
			.setHostname(url.getHost())
			.setPort(port)
			.setConverterFunction(JsonObject::new);

		String username = searchOptions.getUsername();
		String password = searchOptions.getPassword();
		builder.setLogin(username, password);
		builder.setCaPath(searchOptions.getCaPath());
		builder.setCertPath(searchOptions.getCertPath());
		builder.setVerifyHostnames(searchOptions.isHostnameVerification());

		Duration timeout = Duration.ofMillis(searchOptions.getTimeout());

		builder.setConnectTimeout(timeout);
		builder.setReadTimeout(timeout);
		builder.setWriteTimeout(timeout);

		return builder.build();
	}

}
