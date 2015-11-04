package com.gentics.mesh.auth;

import com.gentics.mesh.Mesh;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;

public class MeshJWTAuthProvider implements AuthProvider{

	private MeshAuthProvider meshProvider;
	private JWTAuth jwtProvider;
	
	public MeshJWTAuthProvider() {
		meshProvider = new MeshAuthProvider();
		JsonObject config = new JsonObject().put("keyStore", new JsonObject()
			    .put("path", "keystore.jceks")
			    .put("type", "jceks")
			    .put("password", "secret"));
	
		jwtProvider = JWTAuth.create(Mesh.vertx(), config);
	}
	
	@Override
	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
		meshProvider.authenticate(authInfo, rh -> {
			if (rh.failed()) {
				resultHandler.handle(Future.failedFuture(rh.cause()));
				return;
			}
			User user = rh.result();
			
			JsonObject tokenData = new JsonObject().put("username", user.principal().getString("uuid"));
			
			jwtProvider.generateToken(tokenData, new JWTOptions().setExpiresInSeconds(Mesh.mesh().getOptions().getAuthenticationOptions().getTokenExpirationTime()));
		});
	}
}
