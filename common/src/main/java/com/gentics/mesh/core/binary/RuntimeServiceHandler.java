package com.gentics.mesh.core.binary;

import java.util.Collection;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.service.AuthenticationService;
import com.gentics.mesh.util.StreamUtil;

@Singleton
public class RuntimeServiceHandler {

	private final Set<AuthenticationService> authHandlers;

	@Inject
	public RuntimeServiceHandler() {
		this.authHandlers = StreamUtil.toStream(ServiceLoader.load(AuthenticationService.class)).collect(Collectors.toSet());
	}

	public Collection<AuthenticationService> authHandlers() {
		return Collections.unmodifiableCollection(authHandlers);
	}
}
