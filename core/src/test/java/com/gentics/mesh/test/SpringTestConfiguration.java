package com.gentics.mesh.test;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.TinkerGraphDatabaseProviderImpl;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class SpringTestConfiguration {

	@Bean
	public String graphProviderClassname() {
		return TinkerGraphDatabaseProviderImpl.class.getName();
	}

	@PostConstruct
	public void setup() {
		MeshOptions options = new MeshOptions();
		options.setDatabaseProviderClass(graphProviderClassname());
		options.setHttpPort(TestUtil.getRandomPort());
		Mesh.initalize(options);
	}

}
