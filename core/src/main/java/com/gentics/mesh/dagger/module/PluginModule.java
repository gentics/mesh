package com.gentics.mesh.dagger.module;

import javax.inject.Named;
import javax.inject.Singleton;

import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.impl.OkHttpClientUtil;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

/**
 * Module for plugin specific dependencies.
 */
@Module
public class PluginModule {

	@Provides
	@Singleton
	@Named("pluginClient") 
	public static OkHttpClient pluginOkHttpClient() {
		MeshRestClientConfig config = null;
		return OkHttpClientUtil.createClient(config);
	}
}
