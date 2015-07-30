package com.gentics.mesh.search;

import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfiguration {

	@Bean
	public Node elasticSearchNode() {
		Node node = NodeBuilder.nodeBuilder().node();
		return node;
	}

}
