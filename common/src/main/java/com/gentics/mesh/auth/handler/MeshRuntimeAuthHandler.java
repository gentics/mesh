package com.gentics.mesh.auth.handler;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.core.binary.RuntimeServiceHandler;
import com.gentics.mesh.service.AuthenticationService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthenticationHandlerImpl;

@Singleton
public class MeshRuntimeAuthHandler extends AuthenticationHandlerImpl<MeshJWTAuthProvider> implements MeshAuthHandler {

	private final List<AuthenticationService> runtimeAuthHandlers;

	@Inject
	public MeshRuntimeAuthHandler(MeshJWTAuthProvider authProvider, RuntimeServiceHandler runtimeServiceHandler) {
		super(authProvider);
		this.runtimeAuthHandlers = runtimeServiceHandler.authHandlers().stream().sorted(Comparator.comparingInt(AuthenticationService::priority)).collect(Collectors.toList());
	}

	@Override
	public void authenticate(RoutingContext routingContext, Handler<AsyncResult<User>> handler) {
		// Not needed for this handler
	}

	@Override
	public void handle(RoutingContext rc) {
		runtimeAuthHandlers.stream().map(ah -> ah.handle(rc)).filter(Boolean::booleanValue).findAny().ifPresentOrElse(handled -> {}, () -> rc.next());
	}
}
