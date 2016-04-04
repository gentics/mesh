package com.gentics.mesh.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.gentics.mesh.etc.ElasticSearchOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.ElasticSearchProvider;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
@Profile("test-search")
public class SpringElasticSearchTestConfiguration extends SpringTestConfiguration {

	@Bean
	public SearchProvider searchProvider() {
		ElasticSearchOptions options = new ElasticSearchOptions();
		options.setDirectory("target/elasticsearch_data_" + System.currentTimeMillis());
		SearchProvider provider = new ElasticSearchProvider().init(options);
		return provider;
	}

}
