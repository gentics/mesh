package com.gentics.mesh.auth;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.auth.TokenResponse;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.web.Cookie;

/**
 * Central mesh authentication provider which will handle JWT.
 */
@Singleton
public class MeshAuthProvider implements AuthProvider, JWTAuth {

	private static final Logger log = LoggerFactory.getLogger(MeshAuthProvider.class);

	private JWTAuth jwtProvider;

	public static final String TOKEN_COOKIE_KEY = "mesh.token";

	private static final String USERID_FIELD_NAME = "userUuid";

	private static final String API_KEY_TOKEN_CODE_FIELD_NAME = "jti";

	protected Database db;

	private BCryptPasswordEncoder passwordEncoder;

	private BootstrapInitializer boot;

	@Inject
	public MeshAuthProvider(BCryptPasswordEncoder passwordEncoder, Database database, BootstrapInitializer boot) {
		this.passwordEncoder = passwordEncoder;
		this.db = database;
		this.boot = boot;

		// Use the mesh JWT options in order to setup the JWTAuth provider
		AuthenticationOptions options = Mesh.mesh().getOptions().getAuthenticationOptions();
		String keystorePassword = options.getKeystorePassword();
		if (keystorePassword == null) {
			throw new RuntimeException("The keystore password could not be found within the authentication options.");
		}

		String keyStorePath = options.getKeystorePath();
		String type = "jceks";
		JsonObject config = new JsonObject().put("keyStore", new JsonObject().put("path", keyStorePath).put("type", type).put("password",
				keystorePassword));
		jwtProvider = JWTAuth.create(Mesh.vertx(), config);

	}

	public void authenticateJWT(JsonObject authInfo, Handler<AsyncResult<AuthenticationResult>> resultHandler) {
		if (authInfo.getString("jwt") != null) {
			// Decode and validate the JWT. A JWTUser will be returned which contains the decoded token.
			// We will use this information to load the Mesh User from the graph.
			jwtProvider.authenticate(authInfo, rh -> {
				if (rh.failed()) {
					log.error("Could not authenticate token", rh.cause());
					resultHandler.handle(Future.failedFuture("Invalid Token"));
				} else {
					JsonObject decodedJwt = rh.result().principal();
					try {
						User user = loadUserByJWT(decodedJwt);
						AuthenticationResult result = new AuthenticationResult(user);

						// Check whether an api key was used to authenticate the user.
						if (decodedJwt.containsKey(API_KEY_TOKEN_CODE_FIELD_NAME)) {
							result.setUsingAPIKey(true);
						}
						resultHandler.handle(Future.succeededFuture(result));
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
		// The mesh auth provider is not using this method to generate the token.
		throw new NotImplementedException();
	}

	@Override
	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
		// The mesh auth provider is not using this method to authenticate a user.
		throw new NotImplementedException();
	}

	/**
	 * Authenticates the user and returns a JWToken if successful.
	 *
	 * @param username
	 *            Username
	 * @param password
	 *            Password
	 * @param resultHandler
	 *            Handler to be invoked with the created JWToken
	 */
	public void generateToken(String username, String password, Handler<AsyncResult<String>> resultHandler) {
		authenticate(username, password, rh -> {
			if (rh.failed()) {
				resultHandler.handle(Future.failedFuture(rh.cause()));
			} else {
				User user = rh.result().getUser();
				String uuid = null;
				if (user instanceof MeshAuthUser) {
					uuid = db.tx(() -> ((MeshAuthUser) user).getUuid());
				} else {
					uuid = user.principal().getString("uuid");
				}
				JsonObject tokenData = new JsonObject().put(USERID_FIELD_NAME, uuid);
				resultHandler.handle(Future.succeededFuture(jwtProvider.generateToken(tokenData, new JWTOptions().setExpiresInSeconds(Mesh.mesh()
						.getOptions().getAuthenticationOptions().getTokenExpirationTime()))));
			}
		});
	}

	/**
	 * Load the user with the given username and use the bcrypt encoder to compare the user password with the provided password.
	 *
	 * @param username
	 *            Username
	 * @param password
	 *            Password
	 * @param resultHandler
	 *            Handler which will be invoked which will return the authenticated user or fail if the credentials do not match or the user could not be found
	 */
	private void authenticate(String username, String password, Handler<AsyncResult<AuthenticationResult>> resultHandler) {
		Mesh.vertx().executeBlocking(bh -> {
			try (Tx tx = db.tx()) {
				MeshAuthUser user = boot.userRoot().findMeshAuthUserByUsername(username);
				if (user != null) {
					String accountPasswordHash = user.getPasswordHash();
					// TODO check if user is enabled
					boolean hashMatches = false;
					if (StringUtils.isEmpty(accountPasswordHash) && password != null) {
						if (log.isDebugEnabled()) {
							log.debug("The account password hash or token password string are invalid.");
						}
						bh.fail("Invalid credentials!");
					} else {
						if (log.isDebugEnabled()) {
							log.debug("Validating password using the bcrypt password encoder");
						}
						hashMatches = passwordEncoder.matches(password, accountPasswordHash);
					}
					if (hashMatches) {
						bh.complete(new AuthenticationResult(user));
					} else {
						bh.fail("Invalid credentials!");
					}
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Could not load user with username {" + username + "}.");
					}
					// TODO Don't let the user know that we know that he did not exist?
					bh.fail("Invalid credentials!");
				}
			}
		}, resultHandler);

	}

	/**
	 * Generates a new JWToken with the user.
	 *
	 * @param user
	 *            User
	 * @return The new token
	 */
	public String generateToken(User user) {
		if (user instanceof MeshAuthUser) {
			AuthenticationOptions options = Mesh.mesh().getOptions().getAuthenticationOptions();
			JsonObject tokenData = new JsonObject().put(USERID_FIELD_NAME, ((MeshAuthUser) user).getUuid());
			JWTOptions jwtOptions = new JWTOptions().setAlgorithm(options.getAlgorithm()).setExpiresInSeconds(options.getTokenExpirationTime());
			return jwtProvider.generateToken(tokenData, jwtOptions);
		} else {
			log.error("Can't generate token for user of type {" + user.getClass().getName() + "}");
			throw error(INTERNAL_SERVER_ERROR, "error_internal");
		}
	}

	/**
	 * Generate a new JWT which can be used as an API key.
	 *
	 * @param user
	 * @param tokenCode
	 *            Code which will be part of the JWT. This code is used to verify that the JWT is still valid
	 * @return Generated API key
	 */
	public String generateAPIToken(com.gentics.mesh.core.data.User user, String tokenCode) {
		AuthenticationOptions options = Mesh.mesh().getOptions().getAuthenticationOptions();
		JsonObject tokenData = new JsonObject().put(USERID_FIELD_NAME, user.getUuid()).put(API_KEY_TOKEN_CODE_FIELD_NAME, tokenCode);
		JWTOptions jwtOptions = new JWTOptions().setAlgorithm(options.getAlgorithm());
		return jwtProvider.generateToken(tokenData, jwtOptions);
	}

	/**
	 * Gets the corresponding {@link MeshAuthUser} by the Vert.x User.
	 *
	 * @param jwt
	 *            Decoded JWT
	 * @return Mesh user
	 * @throws Exception
	 */
	private User loadUserByJWT(JsonObject jwt) throws Exception {
		try (Tx tx = db.tx()) {
			String userUuid = jwt.getString(USERID_FIELD_NAME);
			MeshAuthUser user = boot.userRoot().findMeshAuthUserByUuid(userUuid);
			if (user == null) {
				if (log.isDebugEnabled()) {
					log.debug("Could not load user with UUID {" + userUuid + "}.");
				}
				// TODO use NoStackTraceThrowable?
				throw new Exception("Invalid credentials!");
			}
			if (!user.isEnabled()) {
				throw new Exception("User is disabled");
			}

			// Check whether the token might be an API key token
			if (!jwt.containsKey("exp")) {
				String apiKeyToken = jwt.getString(API_KEY_TOKEN_CODE_FIELD_NAME);
				String storedApiKey = user.getAPIKeyTokenCode();
				// Verify that the API token is invalid.
				if (apiKeyToken != null && !apiKeyToken.equals(storedApiKey)) {
					throw new Exception("API key token is invalid.");
				}
			}

			// Load the uuid to cache it
			user.getUuid();
			return user;
		}
	}

	/**
	 * Handle the login action and set a token cookie if the credentials are valid.
	 *
	 * @param ac
	 *            Action context used to add token cookie
	 * @param username
	 *            Username
	 * @param password
	 *            Password
	 */
	public void login(InternalActionContext ac, String username, String password) {
		generateToken(username, password, rh -> {
			if (rh.failed()) {
				throw error(UNAUTHORIZED, "auth_login_failed", rh.cause());
			} else {
				ac.addCookie(Cookie.cookie(MeshAuthProvider.TOKEN_COOKIE_KEY, rh.result()).setMaxAge(Mesh.mesh().getOptions()
						.getAuthenticationOptions().getTokenExpirationTime()).setPath("/"));
				ac.send(JsonUtil.toJson(new TokenResponse(rh.result())));
			}
		});
	}

}
