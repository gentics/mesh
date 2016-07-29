package com.gentics.mesh.auth;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.etc.config.JWTAuthenticationOptions;

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
import rx.Single;

public class MeshJWTAuthProvider extends MeshAuthProvider implements AuthProvider {

	private static final Logger log = LoggerFactory.getLogger(MeshJWTAuthProvider.class);

	private JWTAuth jwtProvider;

	private static final String USERID_FIELD_NAME = "userUuid";

	public MeshJWTAuthProvider() {
		JWTAuthenticationOptions options = Mesh.mesh().getOptions().getAuthenticationOptions().getJwtAuthenticationOptions();
		String secret = options.getSignatureSecret();
		if (secret == null) {
			throw new RuntimeException(
					"Options file is missing the keystore secret password. This should be set in mesh.json: authenticationOptions.signatureSecret");
		}
		JsonObject config = new JsonObject().put("keyStore",
				new JsonObject().put("path", options.getKeystorePath()).put("type", "jceks").put("password", secret));

		jwtProvider = JWTAuth.create(Mesh.vertx(), config);
	}

	@Override
	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
		jwtProvider.authenticate(authInfo, rh -> {
			if (rh.failed()) {
				resultHandler.handle(Future.failedFuture(new VertxException("Invalid Token")));
			} else {
				getUserByJWT(rh.result()).subscribe(user -> {
					resultHandler.handle(Future.succeededFuture(user));
				} , error -> {
					resultHandler.handle(Future.failedFuture(error));
				});
			}
		});
	}

	/**
	 * Gets the {@link MeshAuthUser} by JWT token.
	 * 
	 * @param u
	 * @return
	 */
	private Single<User> getUserByJWT(User u) {
		return db.asyncNoTx(() -> {
			JsonObject authInfo = u.principal();
			String userUuid = authInfo.getString(USERID_FIELD_NAME);
			MeshAuthUser user = boot.userRoot().findMeshAuthUserByUuid(userUuid);
			if (user != null) {
				return Single.just(user);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Could not load user with UUID {" + userUuid + "}.");
				}
				// TODO Don't let the user know that we know that he
				// did not exist?
				throw new Exception("Invalid credentials!");
			}
		});
	}

	/**
	 * Authenticates the user and returns a JWToken if successful.
	 * 
	 * @param username
	 * @param password
	 * @param resultHandler
	 */
	public void generateToken(String username, String password, Handler<AsyncResult<String>> resultHandler) {
		JsonObject authInfo = new JsonObject().put("username", username).put("password", password);
		super.authenticate(authInfo, rh -> {
			if (rh.failed()) {
				resultHandler.handle(Future.failedFuture(rh.cause()));
			} else {
				User user = rh.result();
				JsonObject tokenData = new JsonObject().put(USERID_FIELD_NAME, user.principal().getString("uuid"));

				resultHandler.handle(Future.succeededFuture(jwtProvider.generateToken(tokenData, new JWTOptions().setExpiresInSeconds(
						Mesh.mesh().getOptions().getAuthenticationOptions().getJwtAuthenticationOptions().getTokenExpirationTime()))));
			}
		});
	}

	/**
	 * Generates a new JWToken with the provided user uuid
	 * 
	 * @param userUuid
	 * @return The new token
	 */
	public String generateToken(String userUuid) {
		JsonObject tokenData = new JsonObject().put(USERID_FIELD_NAME, userUuid);

		return jwtProvider.generateToken(tokenData, new JWTOptions()
				.setExpiresInSeconds(Mesh.mesh().getOptions().getAuthenticationOptions().getJwtAuthenticationOptions().getTokenExpirationTime()));
	}

	/**
	 * Generates a new JWToken with the user
	 * 
	 * @param user
	 * @return The new token
	 */
	public String generateToken(User user) {
		return generateToken(user.principal().getString("uuid"));
	}
}
