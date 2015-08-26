package com.gentics.mesh.test;

import javax.annotation.PostConstruct;

import org.elasticsearch.node.Node;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class SpringTestConfiguration {

	@Bean
	public Node elasticSearchNode() {
		// For testing it is not needed to start ES in most cases. This will speedup test execution since ES does not need to initialize.
		return null;
	}

	@PostConstruct
	public void setup() {
		MeshOptions options = new MeshOptions();
		options.getHttpServerOptions().setPort(TestUtil.getRandomPort());
		// The orientdb database provider will switch to in memory mode when no directory has been specified.
		options.getStorageOptions().setDirectory(null);
		Mesh.initalize(options);
	}

}
