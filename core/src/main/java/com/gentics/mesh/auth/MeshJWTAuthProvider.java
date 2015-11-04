package com.gentics.mesh.auth;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.MeshAuthUser;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;

public class MeshJWTAuthProvider extends MeshAuthProvider implements AuthProvider{

	private static final Logger log = LoggerFactory.getLogger(MeshJWTAuthProvider.class);
	
	private JWTAuth jwtProvider;
	
	private static final String USERID_FIELD_NAME = "userUuid";
	
	public MeshJWTAuthProvider() {
		String secret = Mesh.mesh().getOptions().getAuthenticationOptions().getSignatureSecret();
		if (secret == null) {
			throw new RuntimeException("Options file is missing the keystore secret password. This should be set in mesh.json: authenticationOptions.signatureSecret");
		}
		JsonObject config = new JsonObject().put("keyStore", new JsonObject()
			    .put("path", "keystore.jceks")
			    .put("type", "jceks")
			    .put("password", secret));
	
		jwtProvider = JWTAuth.create(Mesh.vertx(), config);
	}
	
	@Override
	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
		String username = authInfo.getString("username");
		String password = authInfo.getString("password");
		
		if (username != null && password != null) {
			super.authenticate(authInfo, resultHandler);
		} else {
			db.asyncNoTrx(tv -> {
				String userUuid = authInfo.getString(USERID_FIELD_NAME);
				MeshAuthUser user = boot.userRoot().findMeshAuthUserByUuid(userUuid);
				if (user != null) {
					resultHandler.handle(Future.succeededFuture(user));
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Could not load user with UUID {" + userUuid + "}.");
					}
					// TODO Don't let the user know that we know that he did not exist?
					resultHandler.handle(Future.failedFuture(new VertxException("Invalid credentials!")));
				}
			}, rh -> {
				if (rh.failed()) {
					log.error("Error while authenticating user.", rh.cause());
					resultHandler.handle(Future.failedFuture(rh.cause()));
				}
			});
		}
	}

	/**
	 * Generates a new JWToken with the provided uuid
	 * @param userUuid
	 * @return The new token
	 */
	public String generateToken(String userUuid) {
		JsonObject tokenData = new JsonObject().put(USERID_FIELD_NAME, userUuid);
		
		return jwtProvider.generateToken(tokenData, new JWTOptions().setExpiresInSeconds(Mesh.mesh().getOptions().getAuthenticationOptions().getTokenExpirationTime()));
	}
	
	/**
	 * Generates a new JWToken with the provided uuid
	 * @param user
	 * @return The new token
	 */
	public String generateToken(User user) {
		return generateToken(user.principal().getString(USERID_FIELD_NAME));
	}
}
