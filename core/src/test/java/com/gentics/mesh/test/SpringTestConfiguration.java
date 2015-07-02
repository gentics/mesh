package com.gentics.mesh.test;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.config.MeshConfiguration;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class SpringTestConfiguration {

	@Bean
	public String graphProviderClassname() {
		return "com.gentics.mesh.util.TinkerGraphDatabaseProviderImpl";
	}

	@PostConstruct
	public void setup() {
		MeshSpringConfiguration.setConfiguration(new MeshConfiguration());
	}

}
