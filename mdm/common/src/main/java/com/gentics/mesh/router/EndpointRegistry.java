package com.gentics.mesh.router;

import com.gentics.mesh.rest.InternalEndpoint;

/**
 * Generic registry for endpoints.
 */
public interface EndpointRegistry {

	/**
	 * Create the given endpoint and register it on all root routers.
	 * 
	 * @param clazz
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	<T extends InternalEndpoint> void register(Class<T> clazz) throws InstantiationException, IllegalAccessException;

}
