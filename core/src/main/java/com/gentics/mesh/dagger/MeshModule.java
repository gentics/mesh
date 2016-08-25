package com.gentics.mesh.dagger;

import static io.vertx.ext.web.handler.SessionHandler.DEFAULT_COOKIE_HTTP_ONLY_FLAG;
import static io.vertx.ext.web.handler.SessionHandler.DEFAULT_COOKIE_SECURE_FLAG;

import javax.inject.Singleton;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.MeshAuthProvider;
import com.gentics.mesh.auth.MeshBasicAuthHandler;
import com.gentics.mesh.auth.MeshJWTAuthHandler;
import com.gentics.mesh.auth.MeshJWTAuthProvider;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.impl.DatabaseHelper;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.image.spi.ImageManipulatorService;
import com.gentics.mesh.core.verticle.auth.AuthenticationRestHandler;
import com.gentics.mesh.core.verticle.auth.BasicAuthRestHandler;
import com.gentics.mesh.core.verticle.auth.JWTAuthRestHandler;
import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.impl.MeshBodyHandlerImpl;
import com.gentics.mesh.image.ImgscalrImageManipulator;

import dagger.Module;
import dagger.Provides;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
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
@Module
public class MeshModule {

	private static final Logger log = LoggerFactory.getLogger(MeshModule.class);

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	@Provides
	@Singleton
	public ImageManipulatorService imageProviderService() {
		return ImageManipulatorService.getInstance();
	}

	@Provides
	@Singleton
	public ImageManipulator imageProvider() {
		// ImageManipulator provider = imageProviderService().getImageProvider();
		// TODO assert provider
		// return provider;
		return new ImgscalrImageManipulator();
	}

	@Provides
	@Singleton
	public static DatabaseService databaseService() {
		return DatabaseService.getInstance();
	}

	@Provides
	@Singleton
	public static Database database() {
		Database database = databaseService().getDatabase();
		if (database == null) {
			String message = "No database provider could be found.";
			log.error(message);
			throw new RuntimeException(message);
		}
		try {
			GraphStorageOptions options = Mesh.mesh().getOptions().getStorageOptions();
			database.init(options, Mesh.vertx(), "com.gentics.mesh.core.data");
			DatabaseHelper helper = new DatabaseHelper(database);
			helper.init();
			return database;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Provides
	@Singleton
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(PASSWORD_HASH_LOGROUND_COUNT);
	}

	@Provides
	@Singleton
	public SessionHandler sessionHandler() {
		SessionStore store = LocalSessionStore.create(Mesh.vertx());
		// TODO make session age configurable
		return new SessionHandlerImpl(MeshOptions.MESH_SESSION_KEY, 30 * 60 * 1000, false, DEFAULT_COOKIE_SECURE_FLAG, DEFAULT_COOKIE_HTTP_ONLY_FLAG,
				store);
	}

	/**
	 * Handler which will authenticate the user credentials.
	 * 
	 * @return
	 */
	@Provides
	@Singleton
	public AuthHandler authHandler(Database db, BootstrapInitializer boot) {
		switch (Mesh.mesh().getOptions().getAuthenticationOptions().getAuthenticationMethod()) {
		case JWT:
			return MeshJWTAuthHandler.create(authProvider(db, boot));
		case BASIC_AUTH:
		default:
			return MeshBasicAuthHandler.create(authProvider(db, boot));
		}
	}

	@Provides
	@Singleton
	public AuthenticationRestHandler authRestHandler(Database db, JWTAuthRestHandler jwtAuthHandler, BasicAuthRestHandler basicAuthHandler) {
		switch (Mesh.mesh().getOptions().getAuthenticationOptions().getAuthenticationMethod()) {
		case JWT:
			return jwtAuthHandler;
		case BASIC_AUTH:
		default:
			return basicAuthHandler;
		}
	}

	/**
	 * User session handler which will provider the user from within the session.
	 * 
	 * @return
	 */
	@Provides
	@Singleton
	public UserSessionHandler userSessionHandler(Database db, BootstrapInitializer boot) {
		return UserSessionHandler.create(authProvider(db, boot));
	}

	/**
	 * Return the mesh auth provider that can be used to authenticate a user.
	 * 
	 * @return
	 */
	@Provides
	@Singleton
	public MeshAuthProvider authProvider(Database db, BootstrapInitializer boot) {
		switch (Mesh.mesh().getOptions().getAuthenticationOptions().getAuthenticationMethod()) {
		case JWT:
			return new MeshJWTAuthProvider(this, db, boot);
		case BASIC_AUTH:
		default:
			return new MeshAuthProvider(this, db, boot);
		}
	}

	/**
	 * Return the configured mail client.
	 * 
	 * @return
	 */
	@Provides
	@Singleton
	public MailClient mailClient() {
		MailConfig config = Mesh.mesh().getOptions().getMailServerOptions();
		MailClient mailClient = MailClient.createShared(Mesh.vertx(), config, "meshClient");
		return mailClient;
	}

	/**
	 * Return the configured CORS handler.
	 * 
	 * @return
	 */
	@Provides
	@Singleton
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
	@Provides
	@Singleton
	public Handler<RoutingContext> bodyHandler() {
		String tempDirectory = Mesh.mesh().getOptions().getUploadOptions().getTempDirectory();
		BodyHandler handler = new MeshBodyHandlerImpl(tempDirectory);
		handler.setBodyLimit(Mesh.mesh().getOptions().getUploadOptions().getByteLimit());
		// TODO check for windows issues
		handler.setUploadsDirectory(tempDirectory);
		return handler;
	}
	

}
