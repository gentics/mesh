package com.gentics.mesh.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.gentics.mesh.etc.ElasticSearchOptions;
import com.gentics.mesh.search.ElasticSearchProvider;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class SpringElasticSearchTestConfiguration extends SpringTestConfiguration {

	@Bean
	public ElasticSearchProvider elasticSearchProvider() {
		ElasticSearchOptions options = new ElasticSearchOptions();
		options.setDirectory("target/elasticsearch_data_" + System.currentTimeMillis());
		return new ElasticSearchProvider().init(options);
	}

}
