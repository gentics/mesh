package com.gentics.mesh.etc;

import static io.vertx.ext.web.handler.SessionHandler.DEFAULT_COOKIE_HTTP_ONLY_FLAG;
import static io.vertx.ext.web.handler.SessionHandler.DEFAULT_COOKIE_SECURE_FLAG;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.MeshAuthProvider;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.ElasticSearchProvider;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.handler.impl.SessionHandlerImpl;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;

/**
 * Main spring bean providing configuration class.
 */
@Configuration
@ComponentScan(basePackages = { "com.gentics.mesh" })
public class MeshSpringConfiguration {

	private static final Logger log = LoggerFactory.getLogger(MeshSpringConfiguration.class);

	public static MeshSpringConfiguration instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static MeshSpringConfiguration getInstance() {
		return instance;
	}

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	@Bean
	public DatabaseService databaseService() {
		return DatabaseService.getInstance();
	}

	@Bean
	public Database database() {
		Database database = databaseService().getDatabase();
		if (database == null) {
			String message = "No database provider could be found.";
			log.error(message);
			throw new RuntimeException(message);
		}
		StorageOptions options = Mesh.mesh().getOptions().getStorageOptions();
		database.init(options, Mesh.vertx());
		return database;
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(PASSWORD_HASH_LOGROUND_COUNT);
	}

	@Bean
	public SearchProvider searchProvider() {
		return new ElasticSearchProvider().init(Mesh.mesh().getOptions().getSearchOptions());
	}

	@Bean
	public SessionHandler sessionHandler() {
		SessionStore store = LocalSessionStore.create(Mesh.vertx());
		return new SessionHandlerImpl("mesh.session", 30 * 60 * 1000, false, DEFAULT_COOKIE_SECURE_FLAG, DEFAULT_COOKIE_HTTP_ONLY_FLAG, store);
	}

	@Bean
	public AuthHandler authHandler() {
		return BasicAuthHandler.create(authProvider(), BasicAuthHandler.DEFAULT_REALM);
	}

	@Bean
	public UserSessionHandler userSessionHandler() {
		return UserSessionHandler.create(authProvider());
	}

	@Bean
	public AuthProvider authProvider() {
		return new MeshAuthProvider();
	}

	@Bean
	public MailClient mailClient() {
		MailConfig config = Mesh.mesh().getOptions().getMailServerOptions();
		MailClient mailClient = MailClient.createShared(Mesh.vertx(), config, "meshClient");
		return mailClient;
	}

	public CorsHandler corsHandler() {
		String pattern = Mesh.mesh().getOptions().getHttpServerOptions().getCorsAllowedOriginPattern();
		CorsHandler corsHandler = CorsHandler.create(pattern);
		corsHandler.allowedMethod(HttpMethod.GET);
		corsHandler.allowedMethod(HttpMethod.POST);
		corsHandler.allowedMethod(HttpMethod.PUT);
		corsHandler.allowedMethod(HttpMethod.DELETE);
		corsHandler.allowedHeader("Authorization");
		corsHandler.allowedHeader("Content-Type");
		return corsHandler;
	}

	// TODO maybe uploads should use a dedicated bodyhandler?
	@Bean
	public Handler<RoutingContext> bodyHandler() {
		BodyHandler handler = BodyHandler.create();
		handler.setBodyLimit(Mesh.mesh().getOptions().getUploadOptions().getByteLimit());
		// TODO check for windows issues
		String tempDirectory = Mesh.mesh().getOptions().getUploadOptions().getTempDirectory();
		handler.setUploadsDirectory(tempDirectory);
		return handler;
	}

}
