package com.gentics.cailun.etc;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gentics.cailun.auth.CaiLunAuthServiceImpl;
import com.gentics.cailun.auth.EnhancedShiroAuthRealmImpl;
import com.gentics.cailun.auth.Neo4jAuthorizingRealm;
import com.gentics.cailun.cli.CaiLunInitializer;
import com.gentics.cailun.etc.config.CaiLunConfiguration;
import com.gentics.cailun.etc.neo4j.UUIDTransactionEventHandler;

@Configuration
@EnableNeo4jRepositories("com.gentics.cailun")
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.gentics.cailun" })
public class CaiLunSpringConfiguration extends Neo4jConfiguration {

	private static final Logger log = LoggerFactory.getLogger(CaiLunSpringConfiguration.class);

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	private static CaiLunConfiguration configuration = null;

	public CaiLunSpringConfiguration() {
		setBasePackage("com.gentics.cailun");
	}

	private void deployNeo4Vertx() throws IOException, InterruptedException {
		log.info("Deploying neo4vertx...");

		final CountDownLatch latch = new CountDownLatch(1);

		// TODO use deployment utils
		vertx().deployVerticle(neo4VertxVerticle(), new DeploymentOptions().setConfig(configuration.getNeo4jConfiguration().getJsonConfig()),
				handler -> {
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
			GraphDatabaseService service = Neo4jGraphVerticle.getDatabase();
			// Add UUID transaction handler
			service.registerTransactionEventHandler(new UUIDTransactionEventHandler(service));
			return Neo4jGraphVerticle.getDatabase();
		} catch (Exception e) {
			log.error("Could not get Neo4J Database from neo4vertx", e);
		}
		return null;
	}

	public static void setConfiguration(CaiLunConfiguration conf) {
		configuration = conf;
	}

	@Bean
	public CaiLunInitializer caiLunInitalizer() {
		return new CaiLunInitializer();
	}

	@PostConstruct
	private void setup() {
		log.debug("Setting up {" + getClass().getCanonicalName() + "}");
		graphDatabaseService();
	}

	@Bean
	public Vertx vertx() {
//		VertxOptions options = new VertxOptions();
		// TODO remove debugging option
//		options.setBlockedThreadCheckPeriod(Long.MAX_VALUE);
		return Vertx.vertx();
	}

	@Bean
	public CaiLunAuthServiceImpl authService() {
		EnhancedShiroAuthRealmImpl realm = new EnhancedShiroAuthRealmImpl(customSecurityRealm());
		CaiLunAuthServiceImpl authService = new CaiLunAuthServiceImpl(vertx(), realm, new JsonObject());
		SecurityUtils.setSecurityManager(realm.getSecurityManager());
		return authService;
	}

	@Bean
	public Neo4jAuthorizingRealm customSecurityRealm() {
		Neo4jAuthorizingRealm realm = new Neo4jAuthorizingRealm();
		realm.setCacheManager(new MemoryConstrainedCacheManager());
		realm.setAuthenticationCachingEnabled(true);
		realm.setCachingEnabled(true);
		return realm;
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(PASSWORD_HASH_LOGROUND_COUNT);
	}

	@Bean
	public RouterStorage routerStorage() {
		return new RouterStorage(vertx(), authService());
	}

}
