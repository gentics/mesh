package com.gentics.mesh.auth.provider;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.handler.MeshJWTAuthHandler;
import io.vertx.core.http.Cookie;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.gentics.mesh.auth.AuthenticationResult;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.auth.TokenResponse;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.shared.SharedKeys;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

/**
 * Central mesh authentication provider which will handle JWT.
 * 
 * Note that the auth proces starts at {@link MeshJWTAuthHandler#handle(io.vertx.ext.web.RoutingContext). The handler will extract the JWT values and this
 * provider will authenticate the data and load the user.
 * 
 */
@Singleton
public class MeshJWTAuthProvider implements AuthenticationProvider, JWTAuth {

	private static final Logger log = LoggerFactory.getLogger(MeshJWTAuthProvider.class);

	private JWTAuth jwtProvider;

	private static final String USERID_FIELD_NAME = "userUuid";

	private static final String API_KEY_TOKEN_CODE_FIELD_NAME = "jti";

	protected final Database db;

	private BCryptPasswordEncoder passwordEncoder;

	private final MeshOptions meshOptions;

	@Inject
	public MeshJWTAuthProvider(Vertx vertx, MeshOptions meshOptions, BCryptPasswordEncoder passwordEncoder, Database database,
		BootstrapInitializer boot) {
		this.meshOptions = meshOptions;
		this.passwordEncoder = passwordEncoder;
		this.db = database;

		// Use the mesh JWT options in order to setup the JWTAuth provider
		AuthenticationOptions options = meshOptions.getAuthenticationOptions();
		String keystorePassword = options.getKeystorePassword();
		if (keystorePassword == null) {
			throw new RuntimeException("The keystore password could not be found within the authentication options.");
		}

		String keyStorePath = options.getKeystorePath();
		String type = "jceks";
		JWTAuthOptions config = new JWTAuthOptions();
		config.setKeyStore(new KeyStoreOptions().setPath(keyStorePath).setPassword(keystorePassword).setType(type));
		jwtProvider = JWTAuth.create(vertx, new JWTAuthOptions(config));
	}

	/**
	 * Authenticate the JWT information and invoke the handler with the result of the authentication process.
	 * 
	 * This method will also load the actual user from the JWT user reference.
	 * 
	 * @param authInfo
	 * @param resultHandler
	 */
	public void authenticateJWT(JsonObject authInfo, Handler<AsyncResult<AuthenticationResult>> resultHandler) {
		// Decode and validate the JWT. A JWTUser will be returned which contains the decoded token.
		// We will use this information to load the Mesh User from the graph.
		jwtProvider.authenticate(authInfo, rh -> {
			if (rh.failed()) {
				if (log.isDebugEnabled()) {
					log.debug("Could not authenticate token.", rh.cause());
				}
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
	}

	@Override
	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
		// The mesh auth provider is not using this method to authenticate a user.
		throw new NotImplementedException();
	}

	@Override
	public String generateToken(JsonObject claims, JWTOptions options) {
		throw new NotImplementedException();
	}

	@Override
	public String generateToken(JsonObject jsonObject) {
		throw new NotImplementedException();
	}

	/**
	 * Authenticates the user and returns a JWToken if successful.
	 *
	 * @param username
	 *            Username
	 * @param password
	 *            Password
	 */
	public String generateToken(String username, String password, String newPassword) {
		HibUser user = authenticate(username, password, newPassword);
		String uuid = db.tx(user::getUuid);
		JsonObject tokenData = new JsonObject().put(USERID_FIELD_NAME, uuid);
		return jwtProvider.generateToken(tokenData, new JWTOptions()
			.setExpiresInSeconds(meshOptions.getAuthenticationOptions().getTokenExpirationTime()));
	}

	/**
	 * Load the user with the given username and use the bcrypt encoder to compare the user password with the provided password.
	 *
	 * @param username
	 *            Username
	 * @param password
	 *            Password
	 */
	private HibUser authenticate(String username, String password, String newPassword) {
		HibUser user = db.tx(tx -> { return tx.userDao().findByUsername(username); });
		if (user != null) {
			String accountPasswordHash = db.tx(user::getPasswordHash);
			// TODO check if user is enabled
			boolean hashMatches = false;
			if (StringUtils.isEmpty(accountPasswordHash) && password != null) {
				if (log.isDebugEnabled()) {
					log.debug("The account password hash or token password string are invalid.");
				}
				throw error(UNAUTHORIZED, "auth_login_failed");
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Validating password using the bcrypt password encoder");
				}
				hashMatches = passwordEncoder.matches(password, accountPasswordHash);
			}
			if (hashMatches) {
				boolean forcedPasswordChange = db.tx(user::isForcedPasswordChange);
				if (forcedPasswordChange && newPassword == null) {
					throw error(BAD_REQUEST, "auth_login_password_change_required");
				} else if (!forcedPasswordChange && newPassword != null) {
					throw error(BAD_REQUEST, "auth_login_newpassword_failed");
				} else {
					if (forcedPasswordChange) {
						db.tx(tx -> { return tx.userDao().setPassword(user, newPassword); });
					}
					return user;
				}
			} else {
				throw error(UNAUTHORIZED, "auth_login_failed");
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Could not load user with username {" + username + "}.");
			}
			// TODO Don't let the user know that we know that he did not exist?
			throw error(UNAUTHORIZED, "auth_login_failed");
		}
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
			AuthenticationOptions options = meshOptions.getAuthenticationOptions();
			JsonObject tokenData = new JsonObject();
			String uuid = db.tx(((MeshAuthUser) user).getDelegate()::getUuid);
			tokenData.put(USERID_FIELD_NAME, uuid);
			JWTOptions jwtOptions = new JWTOptions().setAlgorithm(options.getAlgorithm())
				.setExpiresInSeconds(options.getTokenExpirationTime());
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
	 * @param expireDuration
	 * @return Generated API key
	 */
	public String generateAPIToken(HibUser user, String tokenCode, Integer expireDuration) {
		AuthenticationOptions options = meshOptions.getAuthenticationOptions();
		JsonObject tokenData = new JsonObject()
			.put(USERID_FIELD_NAME, user.getUuid())
			.put(API_KEY_TOKEN_CODE_FIELD_NAME, tokenCode);
		JWTOptions jwtOptions = new JWTOptions().setAlgorithm(options.getAlgorithm());
		if (expireDuration != null) {
			jwtOptions.setExpiresInMinutes(expireDuration);
		}
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
		return db.tx(tx -> {
			String userUuid = jwt.getString(USERID_FIELD_NAME);
			MeshAuthUser user = tx.userDao().findMeshAuthUserByUuid(userUuid);
			if (user == null) {
				if (log.isDebugEnabled()) {
					log.debug("Could not load user with UUID {" + userUuid + "}.");
				}
				// TODO use NoStackTraceThrowable?
				throw new Exception("Invalid credentials!");
			}

			// TODO Re-enable isEnabled cache and check if User#delete behaviour changes
			// if (!user.isEnabled()) {
			// throw new Exception("User is disabled");
			// }

			// Check whether the token might be an API key token
			if (!jwt.containsKey("exp")) {
				String apiKeyToken = jwt.getString(API_KEY_TOKEN_CODE_FIELD_NAME);
				// TODO: All tokens without exp must have a token code - See https://github.com/gentics/mesh/issues/412
				if (apiKeyToken != null) {
					String storedApiKey = user.getDelegate().getAPIKeyTokenCode();
					// Verify that the API token is invalid.
					if (apiKeyToken != null && !apiKeyToken.equals(storedApiKey)) {
						throw new Exception("API key token is invalid.");
					}
				}
			}

			return user;
		});
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
	public void login(InternalActionContext ac, String username, String password, String newPassword) {
		String token = generateToken(username, password, newPassword);
		ac.addCookie(Cookie.cookie(SharedKeys.TOKEN_COOKIE_KEY, token)
			.setMaxAge(meshOptions.getAuthenticationOptions().getTokenExpirationTime()).setPath("/"));
		ac.send(new TokenResponse(token).toJson(ac.isMinify(meshOptions.getHttpServerOptions())));
	}

}
