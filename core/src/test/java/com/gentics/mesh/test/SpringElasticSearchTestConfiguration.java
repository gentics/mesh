package com.gentics.mesh.test;

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
		return NodeBuilder.nodeBuilder().local(true).node();
	}

}
