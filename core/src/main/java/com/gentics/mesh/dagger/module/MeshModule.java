package com.gentics.mesh.dagger.module;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.impl.SearchQueueImpl;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.core.image.spi.ImageManipulatorService;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.DatabaseService;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.impl.MeshBodyHandlerImpl;
import com.gentics.mesh.image.ImgscalrImageManipulator;
import com.gentics.mesh.search.DummySearchProvider;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.storage.BinaryStorageService;

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
	public static ImageManipulatorService imageProviderService() {
		return ImageManipulatorService.getInstance();
	}

	@Provides
	@Singleton
	public static ImageManipulator imageProvider() {
		return new ImgscalrImageManipulator();
	}

	@Provides
	@Singleton
	public static DatabaseService databaseService() {
		return DatabaseService.getInstance();
	}

	@Provides
	@Singleton
	public static BinaryStorageService storageService() {
		return BinaryStorageService.getInstance();
	}

	@Provides
	@Singleton
	public static BinaryStorage binaryStorage() {
		return storageService().getStorage();
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
		return database;
	}

	@Provides
	@Singleton
	public static SearchQueue searchQueue(Provider<SearchQueueBatch> provider) {
		return new SearchQueueImpl(provider);
	}

	@Provides
	@Singleton
	public static BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(PASSWORD_HASH_LOGROUND_COUNT);
	}

	/**
	 * Return the configured CORS handler.
	 * 
	 * @return
	 */
	@Provides
	@Singleton
	public static CorsHandler corsHandler() {
		HttpServerConfig serverOptions = Mesh.mesh().getOptions().getHttpServerOptions();
		String pattern = serverOptions.getCorsAllowedOriginPattern();
		CorsHandler corsHandler = CorsHandler.create(pattern);
		boolean allowCredentials = serverOptions.getCorsAllowCredentials();
		corsHandler.allowCredentials(allowCredentials);
		corsHandler.allowedMethod(HttpMethod.GET);
		corsHandler.allowedMethod(HttpMethod.POST);
		corsHandler.allowedMethod(HttpMethod.PUT);
		corsHandler.allowedMethod(HttpMethod.DELETE);
		corsHandler.allowedHeader("Authorization");
		corsHandler.allowedHeader("Anonymous-Authentication");
		corsHandler.allowedHeader("Content-Type");
		corsHandler.allowedHeader("Accept");
		corsHandler.allowedHeader("Origin");
		corsHandler.allowedHeader("Cookie");
		return corsHandler;
	}

	/**
	 * Return the configured body handler.
	 * 
	 * @return
	 */
	@Provides
	@Singleton
	public static Handler<RoutingContext> bodyHandler() {
		String tempDirectory = Mesh.mesh().getOptions().getUploadOptions().getTempDirectory();
		BodyHandler handler = new MeshBodyHandlerImpl(tempDirectory);
		handler.setBodyLimit(Mesh.mesh().getOptions().getUploadOptions().getByteLimit());
		// TODO check for windows issues
		handler.setUploadsDirectory(tempDirectory);
		handler.setMergeFormAttributes(false);
		return handler;
	}

	/**
	 * Return the configured search provider.
	 * 
	 * @return
	 */
	@Provides
	@Singleton
	public static SearchProvider searchProvider() {
		MeshOptions options = Mesh.mesh().getOptions();
		SearchProvider searchProvider = null;
		// Automatically select the dummy search provider if no directory or
		// options have been specified
		if (options.getSearchOptions() == null || options.getSearchOptions().getDirectory() == null) {
			searchProvider = new DummySearchProvider();
		} else {
			searchProvider = new ElasticSearchProvider();
		}
		return searchProvider;
	}

}
