package com.gentics.mesh.dagger;

import javax.inject.Singleton;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.image.spi.ImageManipulatorService;
import com.gentics.mesh.etc.ElasticSearchOptions;
import com.gentics.mesh.etc.GraphStorageOptions;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.impl.MeshBodyHandlerImpl;
import com.gentics.mesh.image.ImgscalrImageManipulator;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.search.impl.ElasticSearchProvider;

import dagger.Module;
import dagger.Provides;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

/**
 * Main dagger module class.
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

//	@Provides
//	@Singleton
//	public SessionHandler sessionHandler() {
//		SessionStore store = LocalSessionStore.create(Mesh.vertx());
//		// TODO make session age configurable
//		return new SessionHandlerImpl(MeshOptions.MESH_SESSION_KEY, 30 * 60 * 1000, false, DEFAULT_COOKIE_SECURE_FLAG, DEFAULT_COOKIE_HTTP_ONLY_FLAG,
//				store);
//	}

//	/**
//	 * Handler which will authenticate the user credentials.
//	 * 
//	 * @return
//	 */
//	@Provides
//	@Singleton
//	public AuthHandler authHandler(MeshAuthProvider provider) {
////		return MeshJWTAuthHandler.create(provider);
////		return MeshBasicAuthHandler.create(provider);
//	}
//
//	@Provides
//	@Singleton
//	public AuthenticationRestHandler authRestHandler(Database db, JWTAuthRestHandler jwtAuthHandler, BasicAuthRestHandler basicAuthHandler) {
//		return jwtAuthHandler;
//		return basicAuthHandler;
//	}

//	/**
//	 * User session handler which will provider the user from within the session.
//	 * 
//	 * @return
//	 */
//	@Provides
//	@Singleton
//	public UserSessionHandler userSessionHandler(BCryptPasswordEncoder passwordEncoder, Database db) {
//		return UserSessionHandler.create(authProvider(passwordEncoder, db));
//	}
//
//	/**
//	 * Return the mesh auth provider that can be used to authenticate a user.
//	 * 
//	 * @return
//	 */
//	@Provides
//	@Singleton
//	public MeshAuthProvider authProvider(BCryptPasswordEncoder passwordEncoder, Database db) {
//		return new MeshJWTAuthProvider(passwordEncoder, db);
//		return new MeshAuthProvider(passwordEncoder, db);
//	}

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

	/**
	 * Return the configured search provider.
	 * 
	 * @return
	 */
	@Provides
	@Singleton
	public SearchProvider searchProvider() {
		ElasticSearchOptions options = Mesh.mesh().getOptions().getSearchOptions();
		SearchProvider searchProvider = null;
		if (options == null || options.getDirectory() == null) {
			searchProvider = new DummySearchProvider();
		} else {
			searchProvider = new ElasticSearchProvider().init(options);
		}
		return searchProvider;
	}

}
