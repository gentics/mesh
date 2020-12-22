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
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.search.DevNullSearchProvider;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.search.TrackingSearchProviderImpl;
import com.gentics.mesh.search.impl.ElasticSearchProvider;

import dagger.Lazy;
import dagger.Module;
import dagger.Provides;
import io.vertx.core.json.JsonObject;

/**
 * Dagger module which provides the configured search implementation.
 * 
 * Currently three different providers may be injected:
 * 
 * <ul>
 * <li>{@link DevNullSearchProvider} - Noop provider</li>
 * <li>{@link TrackingSearchProvider} - Noop provider which tracks operations (needed for testing)</li>
 * <li>{@link ElasticSearchProvider} - Real Elasticsearch provider which connects to ES</li>
 * </ul>
 */
@Module
public class SearchProviderModule {

	/**
	 * Create the configuration specific search provider. The type of the provider can be injected when the dagger context is created.
	 * 
	 * @param type
	 * @param options
	 * @param elasticsearchProvider
	 * @return
	 */
	@Provides
	@Singleton
	public static SearchProvider searchProvider(@Nullable SearchProviderType type, AbstractMeshOptions options,
		Lazy<ElasticSearchProvider> elasticsearchProvider) {
		if (type == null) {
			if (options.getSearchOptions().getUrl() == null) {
				return new DevNullSearchProvider(options);
			} else {
				return elasticsearchProvider.get();
			}
		}
		switch (type) {
		case NULL:
			return new DevNullSearchProvider(options);
		case TRACKING:
			return new TrackingSearchProviderImpl(options);
		case ELASTICSEARCH:
		default:
			return elasticsearchProvider.get();
		}
	}

	/**
	 * Return the configured ES client to be used to communicate with ES.
	 * 
	 * @param options
	 * @return
	 */
	@Provides
	@Singleton
	public static ElasticsearchClient<JsonObject> searchClient(AbstractMeshOptions options) {

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
