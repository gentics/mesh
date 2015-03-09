package com.gentics.cailun.test;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.graph.neo4j.Neo4VertxConfiguration;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.openpcf.neo4vertx.neo4j.service.GraphService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gentics.cailun.auth.Neo4jAuthorizingRealm;
import com.gentics.cailun.etc.neo4j.UUIDTransactionEventHandler;

@Configuration
@EnableNeo4jRepositories("com.gentics.cailun")
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.gentics.cailun" })
public class SpringTestConfiguration extends Neo4jConfiguration {

	public SpringTestConfiguration() {
		setBasePackage("com.gentics.cailun");
	}

	@Bean
	public GraphDatabaseService graphDatabaseService() {
		GraphDatabaseService service = new TestGraphDatabaseFactory().newImpermanentDatabase();
		service.registerTransactionEventHandler(new UUIDTransactionEventHandler(service));
		Neo4jGraphVerticle.setService(new GraphService() {

			@Override
			public void initialize(Neo4VertxConfiguration configuration) throws Exception {
			}

			@Override
			public GraphDatabaseService getGraphDatabaseService() {
				return service;
			}

			@Override
			public JsonObject query(JsonObject request) throws Exception {
				return null;
			}

			@Override
			public void shutdown() {
			}
			
		});
		return service;
	}
	
	@Bean
	public Neo4jAuthorizingRealm customSecurityRealm() {
		Neo4jAuthorizingRealm realm = new Neo4jAuthorizingRealm();
		realm.setCacheManager(new MemoryConstrainedCacheManager());
		// Disable caching for testing
		realm.setAuthenticationCachingEnabled(false);
		realm.setCachingEnabled(false);
		return realm;
	}

	@Bean
	public Vertx vertx() {
		return Vertx.vertx();
	}

}
