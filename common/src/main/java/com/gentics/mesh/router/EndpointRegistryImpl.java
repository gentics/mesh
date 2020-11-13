package com.gentics.mesh.router;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.rest.InternalEndpoint;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Registry for endpoints. Routes of registered endpoints will automatically be added to all {@link HttpServer} verticle root routers.
 */
@Singleton
public class EndpointRegistryImpl implements EndpointRegistry {

	private static final Logger log = LoggerFactory.getLogger(EndpointRegistryImpl.class);

	private final Vertx vertx;
	private final MeshOptions options;

	private final RouterStorageRegistry routerStorageRegistry;

	@Inject
	public EndpointRegistryImpl(Vertx vertx, MeshOptions options, RouterStorageRegistry routerStorageRegistry) {
		this.vertx = vertx;
		this.options = options;
		this.routerStorageRegistry = routerStorageRegistry;
	}

	@Override
	public <T extends InternalEndpoint> void register(Class<T> clazz) throws InstantiationException, IllegalAccessException {
		for (RouterStorage rs : routerStorageRegistry.getInstances()) {
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
		for (RouterStorage rs : routerStorageRegistry.getInstances()) {
			T endpoint = supplier.get();
			endpoint.init(vertx, rs);
			endpoint.registerEndPoints();
		}
	}
}
