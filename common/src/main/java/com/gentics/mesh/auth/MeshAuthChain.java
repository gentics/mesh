package com.gentics.mesh.auth;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.handler.MeshAnonymousAuthHandler;
import com.gentics.mesh.auth.handler.MeshJWTAuthHandler;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OAuth2Options;

import io.vertx.ext.web.Route;

/**
 * Main location for the Gentics Mesh auth chain.
 */
@Singleton
public class MeshAuthChain {

	private final BootstrapInitializer boot;

	private MeshOAuthService oauthService;

	private MeshJWTAuthHandler jwtAuthHandler;

	private MeshAnonymousAuthHandler anonHandler;

	@Inject
	public MeshAuthChain(BootstrapInitializer boot, MeshOAuthService oauthService, MeshJWTAuthHandler jwtAuthHandler, MeshAnonymousAuthHandler anonHandler) {
		this.boot = boot;
		this.oauthService = oauthService;
		this.jwtAuthHandler = jwtAuthHandler;
		this.anonHandler = anonHandler;
	}

	/**
	 * Secure the given route by adding auth handlers
	 * 
	 * @param route
	 */
	public void secure(Route route) {
		MeshOptions meshOptions = boot.mesh().getOptions();
		OAuth2Options oauthOptions = meshOptions.getAuthenticationOptions().getOauth2();
		if (oauthOptions != null && oauthOptions.isEnabled()) {
			// First try to authenticate the key using JWT
			route.handler(rc -> {
				jwtAuthHandler.handle(rc, true);
			});
			// Now use the Oauth handler which may be able to authenticate the request - The jwt auth handler may have failed
			oauthService.secure(route);
		} else {
			route.handler(rc -> {
				jwtAuthHandler.handle(rc);
			});
		}
		// Finally pass the request through the anonymous handler to fallback to anonymous when enabled
		route.handler(rc -> {
			anonHandler.handle(rc);
		});
	}

}
