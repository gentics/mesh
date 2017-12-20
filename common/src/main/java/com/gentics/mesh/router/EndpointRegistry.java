package com.gentics.mesh.router;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.router.route.AbstractEndpoint;

import io.vertx.core.http.HttpServer;

/**
 * Registry for endpoints. Routes of registered endpoints will automatically be added to all {@link HttpServer} verticle root routers.
 */
@Singleton
public class EndpointRegistry {

	@Inject
	public EndpointRegistry() {
	}

	/**
	 * Create the given endpoint and register it on all root routers.
	 * 
	 * @param clazz
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public <T extends AbstractEndpoint> void register(Class<T> clazz) throws InstantiationException, IllegalAccessException {
		for (RouterStorage rs : RouterStorage.getInstances()) {
			T endpoint = clazz.newInstance();
			endpoint.init(rs);
			endpoint.registerEndPoints();
		}
	}

	/**
	 * Create a new endpoint for each root router and register it.
	 * 
	 * @param provider
	 */
	public <T extends AbstractEndpoint> void register(Provider<T> provider) {
		for (RouterStorage rs : RouterStorage.getInstances()) {
			T endpoint = provider.get();
			endpoint.init(rs);
			endpoint.registerEndPoints();
		}
	}

}
