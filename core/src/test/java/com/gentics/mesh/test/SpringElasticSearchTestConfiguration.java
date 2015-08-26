package com.gentics.mesh.test;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class SpringElasticSearchTestConfiguration extends SpringTestConfiguration {

	@Bean
	public Node elasticSearchNode() {
		String dataDirectory = "target/elasticsearch_data_" + System.currentTimeMillis();
		ImmutableSettings.Builder elasticsearchSettings = ImmutableSettings.settingsBuilder().put("http.enabled", "false").put("path.data",
				dataDirectory);
		Node node = NodeBuilder.nodeBuilder().local(true).settings(elasticsearchSettings.build()).node();
		return node;
	}

}
