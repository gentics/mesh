package com.gentics.mesh.service;

import java.util.Set;

import com.gentics.mesh.rest.InternalEndpoint;
import com.gentics.mesh.router.RouterStorage;

public interface EndpointService {

	Set<InternalEndpoint> generateEndpoints(RouterStorage routerStorageImpl);
}
