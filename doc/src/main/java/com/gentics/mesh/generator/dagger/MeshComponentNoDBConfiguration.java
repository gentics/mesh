package com.gentics.mesh.generator.dagger;

import javax.inject.Singleton;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.TrackingSearchProviderImpl;
import com.gentics.mesh.test.util.TestUtils;

import dagger.Module;
import dagger.Provides;

/**
 * Dagger module for in-memory database setups.
 */
@Module
public class MeshComponentNoDBConfiguration {

	/**
	 * Initialize the mesh settings.
	 */
	public static void init() {
		HibernateMeshOptions options = new HibernateMeshOptions();
		options.getHttpServerOptions().setPort(TestUtils.getRandomPort());
	}

	/**
	 * Return the tracking provider.
	 * 
	 * @param options
	 * @return
	 */
	@Provides
	@Singleton
	public TrackingSearchProviderImpl dummySearchProvider(MeshOptions options) {
		return new TrackingSearchProviderImpl(options);
	}

	/**
	 * Return the dummy provider.
	 * 
	 * @param options
	 * @return
	 */
	@Provides
	@Singleton
	public SearchProvider searchProvider(MeshOptions options) {
		// For testing it is not needed to start ES in most cases. This will speedup test execution since ES does not need to initialize.
		return dummySearchProvider(options);
	}
}
