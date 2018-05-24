package com.gentics.mesh.auth;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.handler.MeshAnonymousAuthHandler;
import com.gentics.mesh.auth.handler.MeshJWTAuthHandler;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OAuth2Options;

import io.vertx.ext.web.Route;

/**
 * Main location for the Gentics Mesh auth chain.
 */
@Singleton
public class MeshAuthChain {

	private MeshOAuthService oauthService;

	private MeshJWTAuthHandler jwtAuthHandler;

	private MeshAnonymousAuthHandler anonHandler;

	@Inject
	public MeshAuthChain(MeshOAuthService oauthService, MeshJWTAuthHandler jwtAuthHandler, MeshAnonymousAuthHandler anonHandler) {
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

		MeshOptions meshOptions = Mesh.mesh().getOptions();
		OAuth2Options oauthOptions = meshOptions.getAuthenticationOptions().getOauth2();
		if (oauthOptions != null && oauthOptions.isEnabled()) {
			route.handler(rc -> {
				jwtAuthHandler.handle(rc, true);
			});
			oauthService.secure(route);
		} else {
			route.handler(rc -> {
				jwtAuthHandler.handle(rc);
			});
		}
		route.handler(rc -> {
			anonHandler.handle(rc);
		});

	}

}
