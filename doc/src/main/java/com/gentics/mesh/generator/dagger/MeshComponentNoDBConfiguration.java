package com.gentics.mesh.generator.dagger;

import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
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
		MeshOptions options = new MeshOptions();
		options.getHttpServerOptions().setPort(TestUtils.getRandomPort());
		// The orientdb database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory(null);
		// Mesh.mesh(options);
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

	/**
	 * Dummy reference to database.
	 * 
	 * @return
	 * @deprecated This method should not be needed. Remove it.
	 */
	@Provides
	@Singleton
	@Deprecated
	public Database database() {
		return null;
	}

}
