package com.gentics.vertx.cailun.auth;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gentics.vertx.cailun.starter.BaseRunner;

@Configuration
@EnableNeo4jRepositories("com.gentics.vertx.cailun")
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.gentics.vertx.cailun" })
public class Neo4jSpringConfiguration extends Neo4jConfiguration {

	private final static Logger log = LoggerFactory.getLogger(Neo4jSpringConfiguration.class);

	public Neo4jSpringConfiguration() {
		setBasePackage("com.gentics.vertx.cailun");
	}

	@Bean
	public Vertx vertx() {
		return Vertx.vertx();
	}

	private void deployNeo4Vertx() throws IOException, InterruptedException {
		log.info("Deploying neo4vertx...");
		InputStream is = BaseRunner.class.getResourceAsStream("neo4vertx_gui.json");
		String jsonTxt = IOUtils.toString(is);
		JsonObject config = new JsonObject(jsonTxt);
		final CountDownLatch latch = new CountDownLatch(1);

		vertx().deployVerticle(Neo4jGraphVerticle.class.getCanonicalName(), new DeploymentOptions().setConfig(config), handler -> {
			log.info("Deployed neo4vertx => " + handler.result());
			latch.countDown();
		});
		latch.await();
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

}
