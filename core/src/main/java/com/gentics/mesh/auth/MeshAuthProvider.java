package com.gentics.mesh.auth;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.auth.TokenResponse;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

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
import io.vertx.ext.web.Cookie;

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

		// Use the mesh jwt options in order to setup the JWTAuth provider
		AuthenticationOptions options = Mesh.mesh().getOptions().getAuthenticationOptions();
		String secret = options.getSignatureSecret();
		if (secret == null) {
			throw new RuntimeException(
					"Options file is missing the keystore secret password. This should be set in mesh configuration file: authenticationOptions.signatureSecret");
		}

		String keyStorePath = options.getKeystorePath();
		String type = "jceks";
		// Copy the demo keystore file to the destination
		if (!new File(keyStorePath).exists()) {
			try {
				InputStream ins = getClass().getResourceAsStream("/keystore.jceks");
				if (ins != null) {
					FileOutputStream fos = new FileOutputStream(keyStorePath);
					IOUtils.copy(ins, fos);
					fos.close();
				}
			} catch (IOException e) {
				log.error("Could not copy keystore for path {" + keyStorePath + "}", e);
			}
		}

		JsonObject config = new JsonObject().put("keyStore", new JsonObject().put("path", keyStorePath).put("type", type).put("password", secret));
		jwtProvider = JWTAuth.create(Mesh.vertx(), config);

	}

	@Override
	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
		if (authInfo.getString("jwt") != null) {
			jwtProvider.authenticate(authInfo, rh -> {
				if (rh.failed()) {
					log.error("Could not authenticate token", rh.cause());
					resultHandler.handle(Future.failedFuture(new VertxException("Invalid Token")));
				} else {

					try {
						User user = getUserByJWT(rh.result());
						resultHandler.handle(Future.succeededFuture(user));
					} catch (Exception e) {
						resultHandler.handle(Future.failedFuture(e));
					}
				}
			});
		} else {
			String username = authInfo.getString("username");
			String password = authInfo.getString("password");
			authenticate(username, password, resultHandler);
		}
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
				resultHandler.handle(Future.succeededFuture(jwtProvider.generateToken(tokenData,
						new JWTOptions().setExpiresInSeconds(Mesh.mesh().getOptions().getAuthenticationOptions().getTokenExpirationTime()))));
			}
		});
	}

	/**
	 * Load the user with the given username and use the bcrypt encoder to compare the user password with the provided password.
	 *
	 * @param username
	 * @param password
	 * @param resultHandler Handler which will be invoked which will return the authenticated user or fail if the credentials do not match or the user could not be found
	 */
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
		return jwtProvider.generateToken(tokenData,
				new JWTOptions().setExpiresInSeconds(Mesh.mesh().getOptions().getAuthenticationOptions().getTokenExpirationTime()));
	}

	/**
	 * Gets the {@link MeshAuthUser} by JWT token.
	 *
	 * @param vertxUser
	 * @return
	 * @throws Exception
	 */
	private User getUserByJWT(User vertxUser) throws Exception {
		try (NoTx noTx = db.noTx()) {
			JsonObject authInfo = vertxUser.principal();
			String userUuid = authInfo.getString(USERID_FIELD_NAME);
			MeshAuthUser user = MeshInternal.get().boot().userRoot().findMeshAuthUserByUuid(userUuid);
			if (user == null) {
				if (log.isDebugEnabled()) {
					log.debug("Could not load user with UUID {" + userUuid + "}.");
				}
				throw new Exception("Invalid credentials!");
			}
			if (!user.isEnabled()) {
				throw new Exception("User is disabled");
			}
			return user;
		}
	}

	/**
	 * Handle the login action and set a token cookie if the credentials are valid.
	 *
	 * @param ac
	 * @param username
	 * @param password
	 */
	public void login(InternalActionContext ac, String username, String password) {
		generateToken(username, password, rh -> {
			if (rh.failed()) {
				throw error(UNAUTHORIZED, "auth_login_failed", rh.cause());
			} else {
				ac.addCookie(Cookie.cookie(MeshAuthProvider.TOKEN_COOKIE_KEY, rh.result())
						.setMaxAge(Mesh.mesh().getOptions().getAuthenticationOptions().getTokenExpirationTime()));
				ac.send(JsonUtil.toJson(new TokenResponse(rh.result())));
			}
		});
	}

}
