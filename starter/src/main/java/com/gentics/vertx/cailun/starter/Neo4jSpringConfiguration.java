package com.gentics.vertx.cailun.starter;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableNeo4jRepositories("com.gentics.vertx.cailun")
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.gentics.vertx.cailun" })
public class Neo4jSpringConfiguration extends Neo4jConfiguration {

	private final static Logger logger = LoggerFactory.getLogger(Neo4jSpringConfiguration.class);

	public Neo4jSpringConfiguration() {
		setBasePackage("com.gentics.vertx.cailun");
	}

	@Bean
	public GraphDatabaseService graphDatabaseService() {
		try {
			return Neo4jGraphVerticle.getDatabase();
		} catch (Exception e) {
			logger.error("Could not get Neo4J Database from neo4vertx", e);
		}
		return null;
	}

}
