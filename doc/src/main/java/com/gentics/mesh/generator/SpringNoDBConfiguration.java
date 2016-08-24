package com.gentics.mesh.generator;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.test.performance.TestUtils;

import dagger.Module;
import dagger.Provides;

@Module
public class SpringNoDBConfiguration extends MeshSpringConfiguration {

	public SpringNoDBConfiguration() {
		MeshOptions options = new MeshOptions();
		options.getHttpServerOptions().setPort(TestUtils.getRandomPort());
		// The orientdb database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory(null);
		Mesh.mesh(options);
	}

	@Provides
	public DummySearchProvider dummySearchProvider() {
		return new DummySearchProvider();
	}

	@Provides
	public SearchProvider searchProvider() {
		// For testing it is not needed to start ES in most cases. This will speedup test execution since ES does not need to initialize.
		return dummySearchProvider();
	}

	@Provides
	public Database database() {
		return null;
	}

}
