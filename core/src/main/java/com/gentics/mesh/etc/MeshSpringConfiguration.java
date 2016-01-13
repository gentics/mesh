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
import com.gentics.mesh.auth.MeshBasicAuthHandler;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.image.spi.ImageManipulatorService;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.impl.MeshBodyHandlerImpl;
import com.gentics.mesh.image.ImgscalrImageManipulator;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.DummySearchProvider;
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
	public ImageManipulatorService imageProviderService() {
		return ImageManipulatorService.getInstance();
	}

	@Bean
	public ImageManipulator imageProvider() {
		//		ImageManipulator provider = imageProviderService().getImageProvider();
		//TODO assert provider
		//		return provider;
		return new ImgscalrImageManipulator();
	}

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
		try {
			GraphStorageOptions options = Mesh.mesh().getOptions().getStorageOptions();
			database.init(options, Mesh.vertx());
			// TODO should we perhaps check the db also within the bootstrap initalizer?
			DatabaseHelper helper = new DatabaseHelper(database);
			helper.init();
			helper.migrate();
			return database;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(PASSWORD_HASH_LOGROUND_COUNT);
	}

	@Bean
	public SearchProvider searchProvider() {
		ElasticSearchOptions options = Mesh.mesh().getOptions().getSearchOptions();
		SearchProvider searchProvider = null;
		if (options == null || options.getDirectory() == null) {
			searchProvider = new DummySearchProvider();
		} else {
			searchProvider = new ElasticSearchProvider().init(Mesh.mesh().getOptions().getSearchOptions());
		}
		return searchProvider;
	}

	@Bean
	public SessionHandler sessionHandler() {
		SessionStore store = LocalSessionStore.create(Mesh.vertx());
		// TODO make session age configurable
		return new SessionHandlerImpl(MeshOptions.MESH_SESSION_KEY, 30 * 60 * 1000, false, DEFAULT_COOKIE_SECURE_FLAG, DEFAULT_COOKIE_HTTP_ONLY_FLAG,
				store);
	}

	/**
	 * Handler which will authenticate the user credentials
	 * 
	 * @return
	 */
	@Bean
	public AuthHandler authHandler() {
		return new MeshBasicAuthHandler(authProvider());
	}

	/**
	 * User session handler which will provider the user from within the session.
	 * 
	 * @return
	 */
	@Bean
	public UserSessionHandler userSessionHandler() {
		return UserSessionHandler.create(authProvider());
	}

	/**
	 * Return the mesh auth provider that can be used to authenticate a user.
	 * 
	 * @return
	 */
	@Bean
	public AuthProvider authProvider() {
		return new MeshAuthProvider();
	}

	/**
	 * Return the configured mail client
	 * 
	 * @return
	 */
	@Bean
	public MailClient mailClient() {
		MailConfig config = Mesh.mesh().getOptions().getMailServerOptions();
		MailClient mailClient = MailClient.createShared(Mesh.vertx(), config, "meshClient");
		return mailClient;
	}

	/**
	 * Return the configured CORS handler
	 * 
	 * @return
	 */
	public CorsHandler corsHandler() {
		String pattern = Mesh.mesh().getOptions().getHttpServerOptions().getCorsAllowedOriginPattern();
		CorsHandler corsHandler = CorsHandler.create(pattern);
		corsHandler.allowedMethod(HttpMethod.GET);
		corsHandler.allowedMethod(HttpMethod.POST);
		corsHandler.allowedMethod(HttpMethod.PUT);
		corsHandler.allowedMethod(HttpMethod.DELETE);
		corsHandler.allowedHeader("Authorization");
		corsHandler.allowedHeader("Content-Type");
		corsHandler.allowedHeader("Set-Cookie");
		return corsHandler;
	}

	/**
	 * Return the configured body handler.
	 * 
	 * @return
	 */
	@Bean
	public Handler<RoutingContext> bodyHandler() {
		String tempDirectory = Mesh.mesh().getOptions().getUploadOptions().getTempDirectory();
		BodyHandler handler = new MeshBodyHandlerImpl(tempDirectory);
		handler.setBodyLimit(Mesh.mesh().getOptions().getUploadOptions().getByteLimit());
		// TODO check for windows issues
		handler.setUploadsDirectory(tempDirectory);
		return handler;
	}

}
