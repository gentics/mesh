package com.gentics.mesh.test;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;
import com.gentics.mesh.search.SearchProvider;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class SpringTestConfiguration {

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
		MeshFactoryImpl.clear();
		MeshOptions options = new MeshOptions();
		options.getHttpServerOptions().setPort(TestUtil.getRandomPort());
		// The database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory(null);
		Mesh.mesh(options);
	}

}
