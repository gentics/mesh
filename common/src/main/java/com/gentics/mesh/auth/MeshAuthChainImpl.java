package com.gentics.mesh.auth;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.handler.MeshAnonymousAuthHandler;
import com.gentics.mesh.auth.handler.MeshJWTAuthHandler;
import com.gentics.mesh.auth.handler.MeshRuntimeAuthHandler;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.ext.web.Route;

/**
 * Main location for the Gentics Mesh auth chain.
 */
@Singleton
public class MeshAuthChainImpl implements MeshAuthChain {

	private final MeshOAuthService oauthService;

	private final MeshJWTAuthHandler jwtAuthHandler;

	private final MeshAnonymousAuthHandler anonHandler;

	private final MeshRuntimeAuthHandler runtimeHandler;

	@Inject
	public MeshAuthChainImpl(MeshOAuthService oauthService, MeshJWTAuthHandler jwtAuthHandler,
		MeshAnonymousAuthHandler anonHandler, MeshRuntimeAuthHandler runtimeHandler, MeshOptions options) {
		this.oauthService = oauthService;
		this.jwtAuthHandler = jwtAuthHandler;
		this.anonHandler = anonHandler;
		this.runtimeHandler = runtimeHandler;
	}

	@Override
	public void secure(Route route) {
		// First try to authenticate the key using JWT
		route.handler(rc -> {
			jwtAuthHandler.handle(rc, true);
		});

		// Now use the OAuth handler which may be able to authenticate the request - The jwt auth handler may have failed
		oauthService.secure(route);

		// Pass the request through the anonymous handler to fallback to anonymous when enabled
		route.handler(rc -> {
			anonHandler.handle(rc);
		});

		// Pass runtime auth handlers, if any exist
		route.handler(rc -> {
			runtimeHandler.handle(rc);
		});
	}

}
