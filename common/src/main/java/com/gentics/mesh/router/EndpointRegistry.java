package com.gentics.mesh.router;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Registry for endpoints. Routes of registered endpoints will automatically be added to all {@link HttpServer} verticle root routers.
 */
@Singleton
public class EndpointRegistry {

	private static final Logger log = LoggerFactory.getLogger(EndpointRegistry.class);

	private final Vertx vertx;
	private final MeshOptions options;

	@Inject
	public EndpointRegistry(Vertx vertx, MeshOptions options) {
		this.vertx = vertx;
		this.options = options;
	}

	/**
	 * Create the given endpoint and register it on all root routers.
	 * 
	 * @param clazz
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public <T extends AbstractInternalEndpoint> void register(Class<T> clazz) throws InstantiationException, IllegalAccessException {
		for (RouterStorage rs : RouterStorage.getInstances()) {
			Constructor<?> constructor;
			try {
				constructor = clazz.getConstructor(new Class[] { MeshOptions.class });
				T endpoint = (T) constructor.newInstance(options);
				endpoint.init(vertx, rs);
				endpoint.registerEndPoints();
			} catch (Exception e) {
				log.error("Registration of endpoint class {" + clazz + "} failed", e);
			}
		}
	}

	/**
	 * Create a new endpoint for each root router and register it.
	 * 
	 * @param provider
	 */
	public <T extends AbstractInternalEndpoint> void register(Supplier<T> supplier) {
		for (RouterStorage rs : RouterStorage.getInstances()) {
			T endpoint = supplier.get();
			endpoint.init(vertx, rs);
			endpoint.registerEndPoints();
		}
	}
}
