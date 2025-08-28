package com.gentics.mesh.dagger.module;

import java.time.Duration;

import javax.inject.Named;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.ProtocolVersion;
import com.gentics.mesh.rest.client.impl.OkHttpClientUtil;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

/**
 * Module for plugin specific dependencies.
 */
@Module
public class PluginModule {

	/**
	 * Return the plugin REST client.
	 *
	 * @param options
	 * @return
	 */
	@Provides
	@Singleton
	@Named("pluginClient")
	public static OkHttpClient pluginOkHttpClient(MeshOptions options) {
		int port = options.getHttpServerOptions().getPort();
		String host = options.getHttpServerOptions().getHost();
		int timeout = options.getPluginTimeout();

		MeshRestClientConfig config = new MeshRestClientConfig.Builder()
			.setHost(host)
			.setPort(port)
			.setSsl(false)
			.setProtocolVersion(options.isPluginUseHttp2() ? ProtocolVersion.HTTP_2 : ProtocolVersion.DEFAULT)
			.setTimeout(Duration.ofSeconds(timeout))
			.build();

		// Create a fresh client for plugins to ensure independence between clients that are used within mesh.
		return OkHttpClientUtil.createClient(config);
	}
}
