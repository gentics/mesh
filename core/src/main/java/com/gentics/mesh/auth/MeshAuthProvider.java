package com.gentics.mesh.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.service.MeshUserService;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.util.BlueprintTransaction;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

@Component
public class MeshAuthProvider implements AuthProvider {

	private static final Logger log = LoggerFactory.getLogger(MeshAuthProvider.class);

	@Autowired
	private MeshUserService userService;

	@Autowired
	private MeshSpringConfiguration springConfiguration;

	@Autowired
	private FramedThreadedTransactionalGraph fg;

	@Autowired
	private Vertx vertx;

	@Override
	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
		vertx.executeBlocking(fut -> {

			String username = authInfo.getString("username");
			String password = authInfo.getString("password");
			MeshAuthUser user;
			try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
				user = userService.findMeshAuthUserByUsername(username);
				tx.success();
			}
			if (user != null) {
				String accountPasswordHash = user.getPasswordHash();
				boolean hashMatches = false;
				if (StringUtils.isEmpty(accountPasswordHash) && password != null) {
					log.debug("The account password hash or token password string are invalid.");
				} else {
					hashMatches = springConfiguration.passwordEncoder().matches(password, accountPasswordHash);
				}
				if (hashMatches) {
					fut.complete(user);
				} else {
					new VertxException("Invalid credentials!");
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Could not load user with username {" + username + "}.");
				}
				// TODO Don't let the user know that we know that he did not exist?
				throw new VertxException("Invalid credentials!");
			}
		}, resultHandler);

	}
}
