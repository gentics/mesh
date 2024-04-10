package com.gentics.mesh.dagger.module;

import javax.inject.Singleton;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.impl.MeshBodyHandlerImpl;
import com.gentics.mesh.image.ImgscalrImageManipulator;

import dagger.Module;
import dagger.Provides;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;

/**
 * Main dagger module class.
 */
@Module
public class MeshModule {

	private static Logger log = LoggerFactory.getLogger(BootstrapInitializer.class);

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	/**
	 * Return the configured image manipulator.
	 * 
	 * @param vertx
	 * @param options
	 * @param boot
	 * @return
	 */
	@Provides
	@Singleton
	public static ImageManipulator imageProvider(io.vertx.reactivex.core.Vertx vertx, MeshOptions options, BinaryStorage binaryStorage, S3BinaryStorage s3BinaryStorage) {
		return new ImgscalrImageManipulator(vertx, options, binaryStorage, s3BinaryStorage);
	}

	/**
	 * Create the password encoder for bcrypt.
	 * 
	 * @return
	 */
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
	public static CorsHandler corsHandler(MeshOptions options) {
		HttpServerConfig serverOptions = options.getHttpServerOptions();
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
	 * Provide the vertex instance from the {@link BootstrapInitializer} since it is initialized there.
	 * 
	 * @param boot
	 * @return
	 */
	@Provides
	public static Vertx vertx(BootstrapInitializer boot) {
		return boot.vertx();
	}

	/**
	 * Return the rx variant of {@link Vertx}
	 * 
	 * @param vertx
	 * @return
	 */
	@Provides
	public static io.vertx.reactivex.core.Vertx rxVertx(Vertx vertx) {
		return new io.vertx.reactivex.core.Vertx(vertx);
	}

	/**
	 * Return the configured body handler.
	 * 
	 * @return
	 */
	@Provides
	@Singleton
	public static BodyHandlerImpl bodyHandler(MeshOptions options) {
		String tempDirectory = options.getUploadOptions().getTempDirectory();
		BodyHandlerImpl handler = new MeshBodyHandlerImpl(tempDirectory);
		handler.setBodyLimit(options.getUploadOptions().getByteLimit());
		// TODO check for windows issues
		handler.setUploadsDirectory(tempDirectory);
		handler.setMergeFormAttributes(false);
		handler.setDeleteUploadedFilesOnEnd(true);
		return handler;
	}

}
