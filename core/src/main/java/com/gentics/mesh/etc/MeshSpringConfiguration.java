package com.gentics.mesh.etc;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.impl.SessionHandlerImpl;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.annotation.PostConstruct;

import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.auth.GraphBackedAuthorizingRealm;
import com.gentics.mesh.auth.MeshShiroAuthProvider;
import com.gentics.mesh.etc.config.MeshConfiguration;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
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
			return service;
		} catch (Exception e) {
			log.error("Could not get Neo4J Database from neo4vertx", e);
		}
		return null;
	}

	@Bean
	public FramedGraph getFramedGraph() {
		Neo4j2Graph graph = new Neo4j2Graph(graphDatabaseService());
		//TODO configure indices
		graph.createKeyIndex("ferma_type", Vertex.class);
		graph.createKeyIndex("uuid", Vertex.class);
		graph.createKeyIndex("ferma_type", Edge.class);
		graph.createKeyIndex("uuid", Edge.class);
		graph.createKeyIndex("languageTag", Edge.class);
		graph.createKeyIndex("languageTag", Vertex.class);
		graph.createKeyIndex("name", Vertex.class);
		graph.createKeyIndex("key", Vertex.class);
		FramedGraph framedGraph = new DelegatingFramedTransactionalGraph<Neo4j2Graph>(graph, true, false);
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

		GraphBackedAuthorizingRealm realm = new GraphBackedAuthorizingRealm();
		realm.setCacheManager(new MemoryConstrainedCacheManager());
		realm.setAuthenticationCachingEnabled(true);
		realm.setCachingEnabled(true);

		return new MeshShiroAuthProvider(vertx(), realm);
	}

	public CorsHandler corsHandler() {
		//TODO make core configurable
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

	//TODO maybe uploads should use a dedicated bodyhandler?
	@Bean
	public Handler<RoutingContext> bodyHandler() {
		BodyHandler handler = BodyHandler.create();
		handler.setBodyLimit(MeshSpringConfiguration.getConfiguration().getFileUploadByteLimit());
		//TODO check for windows issues 
		handler.setUploadsDirectory("target/" + BodyHandler.DEFAULT_UPLOADS_DIRECTORY);
		return handler;
	}

}
