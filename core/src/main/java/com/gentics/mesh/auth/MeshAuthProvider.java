package com.gentics.mesh.auth;

import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.JWTAuthenticationOptions;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;

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

/**
 * Mesh authentication provider.
 */
@Singleton
public class MeshAuthProvider implements AuthProvider, JWTAuth {

	private static final Logger log = LoggerFactory.getLogger(MeshAuthProvider.class);

	private JWTAuth jwtProvider;

	public static final String TOKEN_COOKIE_KEY = "mesh.token";

	private static final String USERID_FIELD_NAME = "userUuid";

	protected Database db;

	private BCryptPasswordEncoder passwordEncoder;

	@Inject
	public MeshAuthProvider(BCryptPasswordEncoder passwordEncoder, Database database) {
		this.passwordEncoder = passwordEncoder;
		this.db = database;

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
				resultHandler.handle(Future.succeededFuture(getUserByJWT(rh.result())));
			}
		});
	}

	/**
	 * @deprecated Use {@link MeshAuthProvider#generateToken(User)} instead
	 */
	@Override
	public String generateToken(JsonObject arg0, JWTOptions arg1) {
		// The Mesh Auth Provider is not using this method to generate the token.
		throw new NotImplementedException();
	}

	/**
	 * Authenticates the user and returns a JWToken if successful.
	 * 
	 * @param username
	 * @param password
	 * @param resultHandler
	 */
	public void generateToken(String username, String password, Handler<AsyncResult<String>> resultHandler) {
		authenticate(username, password, rh -> {
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

	private void authenticate(String username, String password, Handler<AsyncResult<User>> resultHandler) {
		try (NoTx noTx = db.noTx()) {
			MeshAuthUser user = MeshInternal.get().boot().userRoot().findMeshAuthUserByUsername(username);
			if (user != null) {
				String accountPasswordHash = user.getPasswordHash();
				// TODO check if user is enabled
				boolean hashMatches = false;
				if (StringUtils.isEmpty(accountPasswordHash) && password != null) {
					if (log.isDebugEnabled()) {
						log.debug("The account password hash or token password string are invalid.");
					}
					resultHandler.handle(Future.failedFuture(new VertxException("Invalid credentials!")));
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Validating password using the bcrypt password encoder");
					}
					hashMatches = passwordEncoder.matches(password, accountPasswordHash);
				}
				if (hashMatches) {
					resultHandler.handle(Future.succeededFuture(user));
				} else {
					resultHandler.handle(Future.failedFuture(new VertxException("Invalid credentials!")));
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Could not load user with username {" + username + "}.");
				}
				// TODO Don't let the user know that we know that he did not exist?
				resultHandler.handle(Future.failedFuture(new VertxException("Invalid credentials!")));
			}

			// , rh -> {
			// if (rh.failed()) {
			// log.error("Error while authenticating user.", rh.cause());
			// resultHandler.handle(Future.failedFuture(rh.cause()));
			// }
			// });
		}

	}

	/**
	 * Generates a new JWToken with the user.
	 * 
	 * @param user
	 * @return The new token
	 */
	public String generateToken(User user) {
		JsonObject tokenData = new JsonObject().put(USERID_FIELD_NAME, user.principal().getString("uuid"));
		return jwtProvider.generateToken(tokenData, new JWTOptions()
				.setExpiresInSeconds(Mesh.mesh().getOptions().getAuthenticationOptions().getJwtAuthenticationOptions().getTokenExpirationTime()));
	}

	/**
	 * Gets the {@link MeshAuthUser} by JWT token.
	 *
	 * @param user
	 * @return
	 */
	private User getUserByJWT(User vertxUser) {
		try (NoTx noTx = db.noTx()) {
			JsonObject authInfo = vertxUser.principal();
			String userUuid = authInfo.getString(USERID_FIELD_NAME);
			return MeshInternal.get().boot().userRoot().findMeshAuthUserByUuid(userUuid);
			// if (user != null) {
			// return user;
			// } else {
			// if (log.isDebugEnabled()) {
			// log.debug("Could not load user with UUID {" + userUuid + "}.");
			// }
			// // TODO Don't let the user know that we know that he
			// // did not exist?
			// throw new Exception("Invalid credentials!");
			// }
		}
	}

}
