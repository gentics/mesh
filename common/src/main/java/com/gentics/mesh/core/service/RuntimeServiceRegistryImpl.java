package com.gentics.mesh.core.service;

import java.util.Collection;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.handler.RuntimeServiceRegistry;
import com.gentics.mesh.service.AuthenticationService;
import com.gentics.mesh.service.EndpointService;
import com.gentics.mesh.util.StreamUtil;

@Singleton
public class RuntimeServiceRegistryImpl implements RuntimeServiceRegistry {

	private final Set<AuthenticationService> authHandlers;
	private final Set<EndpointService> endpointHandlers;

	@Inject
	public RuntimeServiceRegistryImpl() {
		this.authHandlers = StreamUtil.toStream(ServiceLoader.load(AuthenticationService.class)).collect(Collectors.toSet());
		this.endpointHandlers = StreamUtil.toStream(ServiceLoader.load(EndpointService.class)).collect(Collectors.toSet());
	}

	@Override
	public Collection<AuthenticationService> authHandlers() {
		return Collections.unmodifiableCollection(authHandlers);
	}

	@Override
	public Collection<EndpointService> endpointHandlers() {
		return Collections.unmodifiableCollection(endpointHandlers);
	}
}
