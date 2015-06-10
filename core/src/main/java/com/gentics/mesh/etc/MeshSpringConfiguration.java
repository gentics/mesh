package com.gentics.mesh.etc;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.handler.AuthHandler;
import io.vertx.ext.apex.handler.BasicAuthHandler;
import io.vertx.ext.apex.handler.BodyHandler;
import io.vertx.ext.apex.handler.CorsHandler;
import io.vertx.ext.apex.handler.SessionHandler;
import io.vertx.ext.apex.handler.impl.SessionHandlerImpl;
import io.vertx.ext.apex.sstore.LocalSessionStore;
import io.vertx.ext.apex.sstore.SessionStore;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.shiro.impl.ShiroAuthProviderImpl;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;

import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.jglue.totorom.FramedGraph;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.auth.EnhancedShiroAuthRealmImpl;
import com.gentics.mesh.auth.GraphBackedAuthorizingRealm;
import com.gentics.mesh.etc.config.MeshConfiguration;
import com.tinkerpop.blueprints.impls.neo4j2.Neo4j2Graph;

@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
@EnableAspectJAutoProxy
public class MeshSpringConfiguration {

	private static final Logger log = LoggerFactory.getLogger(MeshSpringConfiguration.class);

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	private static MeshConfiguration configuration = null;

	private void deployNeo4Vertx() throws IOException, InterruptedException {
		log.info("Deploying neo4vertx...");

		final CountDownLatch latch = new CountDownLatch(1);

		// TODO use deployment utils
		vertx().deployVerticle(neo4VertxVerticle(), new DeploymentOptions().setConfig(configuration.getNeo4jConfiguration().getJsonConfig()),
				handler -> {
					log.info("Deployed neo4vertx => " + handler.result());
					if (handler.failed()) {
						log.error("Could not deploy neo4vertx. Aborting..");
						log.error("Error:", handler.cause());
						/* TODO safe exit */
						System.exit(10);
					} else {
						log.info("Neo4Vertx deployed successfully");
					}

					// TODO handle exceptions
				latch.countDown();
			});
		latch.await();
	}

	@Bean
	public Neo4jGraphVerticle neo4VertxVerticle() {
		return new Neo4jGraphVerticle();
	}

	private void registerShutdownHook(final GraphDatabaseService graphDatabaseService) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDatabaseService.shutdown();
			}
		});
	}

	@Bean
	public GraphDatabaseService graphDatabaseService() {
		try {
			deployNeo4Vertx();
			GraphDatabaseService service = Neo4jGraphVerticle.getService().getGraphDatabaseService();
			// Add UUID transaction handler that injects uuid in new neo4j nodes and relationships
			//			service.registerTransactionEventHandler(new UUIDTransactionEventHandler(service));
			return service;
		} catch (Exception e) {
			log.error("Could not get Neo4J Database from neo4vertx", e);
		}
		return null;
	}

	@Bean
	public FramedGraph getFramedGraph() {
		Neo4j2Graph graph = new Neo4j2Graph(graphDatabaseService());
		FramedGraph framedGraph = new FramedGraph(graph);
		return framedGraph;
	}

	public static MeshConfiguration getConfiguration() {
		return configuration;
	}

	public static void setConfiguration(MeshConfiguration conf) {
		configuration = conf;
	}

	@PostConstruct
	private void setup() {
		log.debug("Setting up {" + getClass().getCanonicalName() + "}");
		graphDatabaseService();
	}

	@Bean
	public Vertx vertx() {
		VertxOptions options = new VertxOptions();
		options.setBlockedThreadCheckPeriod(1000 * 60 * 60);
		return Vertx.vertx(options);
	}

	@Bean
	public GraphBackedAuthorizingRealm customSecurityRealm() {
		GraphBackedAuthorizingRealm realm = new GraphBackedAuthorizingRealm();
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
	public SessionHandler sessionHandler() {
		SessionStore store = LocalSessionStore.create(vertx());
		return new SessionHandlerImpl("mesh.session", 30 * 60 * 1000, false, store);
	}

	@Bean
	public AuthHandler authHandler() {
		return BasicAuthHandler.create(authProvider(), BasicAuthHandler.DEFAULT_REALM);
	}

	@Bean
	public AuthProvider authProvider() {
		EnhancedShiroAuthRealmImpl realm = new EnhancedShiroAuthRealmImpl(customSecurityRealm());
		// ExposingShiroAuthProvider provider = new ExposingShiroAuthProvider(vertx(), realm);
		// MeshAuthServiceImpl authService = new MeshAuthServiceImpl(vertx(), new JsonObject(), provider);
		// SecurityUtils.setSecurityManager(realm.getSecurityManager());
		return new ShiroAuthProviderImpl(vertx(), realm);
	}

	public CorsHandler corsHandler() {
		CorsHandler corsHandler = CorsHandler.create("*");
		// corsHandler.allowCredentials(true);
		corsHandler.allowedMethod(HttpMethod.GET);
		corsHandler.allowedMethod(HttpMethod.POST);
		corsHandler.allowedMethod(HttpMethod.PUT);
		corsHandler.allowedMethod(HttpMethod.DELETE);
		corsHandler.allowedHeader("Authorization");
		corsHandler.allowedHeader("Content-Type");
		return corsHandler;
	}

	@Bean
	public Handler<RoutingContext> bodyHandler() {
		BodyHandler handler = BodyHandler.create();
		handler.setUploadsDirectory("target/" + BodyHandler.DEFAULT_UPLOADS_DIRECTORY);
		return handler;
	}

}
