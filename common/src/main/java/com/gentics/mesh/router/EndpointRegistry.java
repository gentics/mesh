package com.gentics.mesh.router;

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.router.route.AbstractInternalEndpoint;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

/**
 * Registry for endpoints. Routes of registered endpoints will automatically be added to all {@link HttpServer} verticle root routers.
 */
@Singleton
public class EndpointRegistry {

	private final Vertx vertx;

	@Inject
	public EndpointRegistry(Vertx vertx) {
		this.vertx = vertx;
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
			T endpoint = clazz.newInstance();
			endpoint.init(vertx, rs);
			endpoint.registerEndPoints();
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
