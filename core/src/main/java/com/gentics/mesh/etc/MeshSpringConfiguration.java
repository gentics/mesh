package com.gentics.mesh.etc;

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

import javax.annotation.PostConstruct;

import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.neo4j.cluster.ClusterSettings;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.HighlyAvailableGraphDatabaseFactory;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.ha.HaSettings;
import org.neo4j.server.Bootstrapper;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gentics.mesh.auth.EnhancedShiroAuthRealmImpl;
import com.gentics.mesh.auth.Neo4jAuthorizingRealm;
import com.gentics.mesh.etc.config.MeshConfiguration;
import com.gentics.mesh.etc.config.MeshNeo4jConfiguration;
import com.gentics.mesh.etc.neo4j.UUIDTransactionEventHandler;

@Configuration
@EnableNeo4jRepositories("com.gentics.mesh")
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.gentics.mesh" })
@EnableAspectJAutoProxy
public class MeshSpringConfiguration extends Neo4jConfiguration {

	private static final Logger log = LoggerFactory.getLogger(MeshSpringConfiguration.class);

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	private static MeshConfiguration configuration = null;

	public MeshSpringConfiguration() {
		setBasePackage("com.gentics.mesh");
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
	public GraphDatabaseService graphDatabaseService() throws Exception {
		MeshNeo4jConfiguration neo4jConfig = configuration.getNeo4jConfiguration();
		final String mode = neo4jConfig.getMode();

		GraphDatabaseService service = null;
		switch (mode) {
		case MeshNeo4jConfiguration.DEFAULT_MODE:
			service = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(neo4jConfig.getPath()).newGraphDatabase();
			break;
		case MeshNeo4jConfiguration.CLUSTER_MODE:
			GraphDatabaseBuilder builder = new HighlyAvailableGraphDatabaseFactory().newHighlyAvailableDatabaseBuilder(neo4jConfig.getPath());

			// Set various HA settings we support
			builder.setConfig(ClusterSettings.server_id, neo4jConfig.getHAServerID());
			builder.setConfig(HaSettings.ha_server, neo4jConfig.getHAServer());
			builder.setConfig(HaSettings.slave_only, String.valueOf(neo4jConfig.getHASlaveOnly()));
			builder.setConfig(ClusterSettings.cluster_server, neo4jConfig.getHAClusterServer());
			builder.setConfig(ClusterSettings.initial_hosts, neo4jConfig.getHAInitialHosts());

			GraphDatabaseService graphDatabaseService = builder.newGraphDatabase();
			registerShutdownHook(graphDatabaseService);
			break;
		case MeshNeo4jConfiguration.GUI_MODE:
			service = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(neo4jConfig.getPath()).newGraphDatabase();
			ServerConfigurator webConfig = new ServerConfigurator((GraphDatabaseAPI) service);
			webConfig.configuration().setProperty(Configurator.WEBSERVER_ADDRESS_PROPERTY_KEY, neo4jConfig.getWebServerBindAddress());
			Bootstrapper bootStrapper = new WrappingNeoServerBootstrapper((GraphDatabaseAPI) service, webConfig);
			bootStrapper.start();
			break;
		default:
			throw new Exception("Invalid mode " + mode + " specified");
		}
		try {
			// Add UUID transaction handler that injects uuid in new neo4j nodes and relationships
			service.registerTransactionEventHandler(new UUIDTransactionEventHandler(service));
			return service;
		} catch (Exception e) {
			log.error("Could not get Neo4J Database from neo4vertx", e);
		}
		return null;
	}

	public static MeshConfiguration getConfiguration() {
		return configuration;
	}

	public static void setConfiguration(MeshConfiguration conf) {
		configuration = conf;
	}

	@PostConstruct
	private void setup() throws Exception {
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
