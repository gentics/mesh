package com.gentics.mesh.util;

import java.util.function.Consumer;

import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

public class JWTTestUtil {

	private JWTTestUtil() {}

	public static Tuple<JWTAuth, JWTOptions> createAuth(Vertx vertx, MeshOptions options, Consumer<JWTOptions> jwtChanger) {
		String keyStorePath = options.getAuthenticationOptions().getKeystorePath();
		String keystorePassword = options.getAuthenticationOptions().getKeystorePassword();
		String type = "jceks";
		JWTAuthOptions config = new JWTAuthOptions();
		JWTOptions jwtOptions = JWTUtil.createJWTOptions(options.getAuthenticationOptions());
		if (jwtChanger != null) {
			jwtChanger.accept(jwtOptions);
		}

		// Set JWT options from the config
		config.setJWTOptions(jwtOptions);
		config.setKeyStore(new KeyStoreOptions().setPath(keyStorePath).setPassword(keystorePassword).setType(type));
		JWTAuth jwtProvider = JWTAuth.create(vertx, new JWTAuthOptions(config));

		return Tuple.tuple(jwtProvider, jwtOptions);
	}
}
