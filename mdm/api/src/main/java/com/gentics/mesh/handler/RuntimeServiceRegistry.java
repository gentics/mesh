package com.gentics.mesh.handler;

import java.util.Collection;

import com.gentics.mesh.service.AuthenticationService;
import com.gentics.mesh.service.EndpointService;

/**
 * Registry for the runtime classpath services.
 */
public interface RuntimeServiceRegistry {

	/**
	 * Get auth handle services.
	 * 
	 * @return
	 */
	Collection<AuthenticationService> authHandlers();

	/**
	 * Get routing endpoint services.
	 * 
	 * @return
	 */
	Collection<EndpointService> endpointHandlers();

	/**
	 * Reset the registry.
	 */
	void reset();
}