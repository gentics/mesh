package com.gentics.mesh.auth;

import com.gentics.mesh.auth.handler.MeshAnonymousAuthHandler;
import com.gentics.mesh.auth.handler.MeshJWTAuthHandler;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.ext.web.Route;

/**
 * Main location for the Gentics Mesh auth chain.
 */
public class MeshAuthChainImpl implements MeshAuthChain {

	private final MeshOAuthService oauthService;

	private final MeshJWTAuthHandler jwtAuthHandler;

	private final MeshAnonymousAuthHandler anonHandler;

	public MeshAuthChainImpl(MeshOAuthService oauthService, MeshJWTAuthHandler jwtAuthHandler,
		MeshAnonymousAuthHandler anonHandler, MeshOptions options) {
		this.oauthService = oauthService;
		this.jwtAuthHandler = jwtAuthHandler;
		this.anonHandler = anonHandler;
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
	}
}
