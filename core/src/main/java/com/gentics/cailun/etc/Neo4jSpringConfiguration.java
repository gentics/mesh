package com.gentics.cailun.etc;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gentics.cailun.etc.config.CaiLunConfiguration;

@Configuration
@EnableNeo4jRepositories("com.gentics.cailun")
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.gentics.cailun" })
public class Neo4jSpringConfiguration extends Neo4jConfiguration {

	private final static Logger log = LoggerFactory.getLogger(Neo4jSpringConfiguration.class);

	private static CaiLunConfiguration configuration = null;

	public Neo4jSpringConfiguration() {
		setBasePackage("com.gentics.cailun");
	}

	@Bean
	public Vertx vertx() {
		return Vertx.vertx();
	}

	private void deployNeo4Vertx() throws IOException, InterruptedException {
		log.info("Deploying neo4vertx...");

		final CountDownLatch latch = new CountDownLatch(1);

		// TODO use deployment utils
		vertx().deployVerticle(neo4VertxVerticle(), new DeploymentOptions().setConfig(configuration.getNeo4jConfiguration().getJsonConfig()), handler -> {
			log.info("Deployed neo4vertx => " + handler.result());
			// TODO handle exceptions
				latch.countDown();
			});
		latch.await();
	}

	@Bean
	public Neo4jGraphVerticle neo4VertxVerticle() {
		return new Neo4jGraphVerticle();
	}

	@Bean
	public GraphDatabaseService graphDatabaseService() {
		try {
			deployNeo4Vertx();
			return Neo4jGraphVerticle.getDatabase();
		} catch (Exception e) {
			log.error("Could not get Neo4J Database from neo4vertx", e);
		}
		return null;
	}

	public static void setConfiguration(CaiLunConfiguration conf) {
		configuration = conf;
	}

}
