package com.gentics.mesh.handler;

import java.util.Collection;

import com.gentics.mesh.service.AuthenticationService;
import com.gentics.mesh.service.EndpointService;

public interface RuntimeServiceRegistry {

	Collection<AuthenticationService> authHandlers();

	Collection<EndpointService> endpointHandlers();

}