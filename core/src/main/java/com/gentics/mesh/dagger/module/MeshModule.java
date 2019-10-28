package com.gentics.mesh.dagger.module;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.image.spi.ImageManipulator;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.impl.MeshBodyHandlerImpl;
import com.gentics.mesh.image.ImgscalrImageManipulator;
import com.gentics.mesh.util.StreamUtil;

import dagger.Module;
import dagger.Provides;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
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

	private static final UUID JvmId = UUID.randomUUID();

	private static final int PASSWORD_HASH_LOGROUND_COUNT = 10;

	@Provides
	@Singleton
	public static ImageManipulator imageProvider(io.vertx.reactivex.core.Vertx vertx, MeshOptions options) {
		return new ImgscalrImageManipulator(vertx, options);
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

	@Provides
	public static Vertx vertx(BootstrapInitializer boot) {
		return boot.vertx();
	}

	@Provides
	public static io.vertx.reactivex.core.Vertx rxVertx(Vertx vertx) {
		return new io.vertx.reactivex.core.Vertx(vertx);
	}

	@Provides
	@Singleton
	public static MeterRegistry meterRegistry(MeshOptions options) {
		PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

		Iterable<Tag> tags = Stream.of(
			createTag("nodeName", options.getNodeName()),
			createTag("jvmId", JvmId.toString())
		).flatMap(StreamUtil::toStream)::iterator;

		registry.config().commonTags(tags);

		return registry;
	}

	private static Optional<Tag> createTag(String key, String value) {
		if (key == null || value == null) {
			return Optional.empty();
		} else {
			return Optional.of(Tag.of(key, value));
		}
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
