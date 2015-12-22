package com.gentics.mesh.auth;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.etc.MeshSpringConfiguration;
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

/**
 * Mesh auth provider
 */
@Component
public class MeshAuthProvider implements AuthProvider {

	private static final Logger log = LoggerFactory.getLogger(MeshAuthProvider.class);

	@Autowired
	private BootstrapInitializer boot;

	@Autowired
	private Database db;

	@Autowired
	private MeshSpringConfiguration springConfiguration;

	@Override
	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
		db.asyncNoTrx(() -> {
			String username = authInfo.getString("username");
			String password = authInfo.getString("password");
			MeshAuthUser user = boot.userRoot().findMeshAuthUserByUsername(username);
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
					hashMatches = springConfiguration.passwordEncoder().matches(password, accountPasswordHash);
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
			
//			 , rh -> {
//					if (rh.failed()) {
//						log.error("Error while authenticating user.", rh.cause());
//						resultHandler.handle(Future.failedFuture(rh.cause()));
//					}
//				});
			return null;
		});

	}
}
