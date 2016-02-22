package com.gentics.mesh.generator;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.test.TestUtils;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class SpringNoDBConfiguration {

	@Bean
	public DummySearchProvider dummySearchProvider() {
		return new DummySearchProvider();
	}

	@Bean
	public SearchProvider searchProvider() {
		// For testing it is not needed to start ES in most cases. This will speedup test execution since ES does not need to initialize.
		return dummySearchProvider();
	}

	@PostConstruct
	public void setup() {
		MeshOptions options = new MeshOptions();
		options.getHttpServerOptions().setPort(TestUtils.getRandomPort());
		// The orientdb database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory(null);
		Mesh.mesh(options);
	}

	@Bean
	public Database database() {
		return null;
	}

}
