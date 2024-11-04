package com.gentics.mesh.dagger.module;

import javax.inject.Singleton;

import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.auth.MeshAuthChainImpl;
import com.gentics.mesh.auth.MeshOAuthService;
import com.gentics.mesh.auth.handler.MeshAnonymousAuthHandler;
import com.gentics.mesh.auth.handler.MeshJWTAuthHandler;
import com.gentics.mesh.etc.config.MeshOptions;

import dagger.Module;
import dagger.Provides;

@Module
public class AuthChainProviderModule {

	@Provides
	@Singleton
	public static MeshAuthChain authChain(MeshOAuthService oauthService, MeshJWTAuthHandler jwtAuthHandler,
			MeshAnonymousAuthHandler anonHandler, MeshOptions options) {
		return new MeshAuthChainImpl(oauthService, jwtAuthHandler, anonHandler, options);
	}
}
