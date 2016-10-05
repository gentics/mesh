//package com.gentics.mesh.auth;
//
//import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;
//
//import javax.inject.Inject;
//import javax.inject.Singleton;
//
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//
//import com.gentics.mesh.core.data.MeshAuthUser;
//import com.gentics.mesh.dagger.MeshInternal;
//import com.gentics.mesh.graphdb.spi.Database;
//
//import io.vertx.core.AsyncResult;
//import io.vertx.core.Future;
//import io.vertx.core.Handler;
//import io.vertx.core.VertxException;
//import io.vertx.core.json.JsonObject;
//import io.vertx.core.logging.Logger;
//import io.vertx.core.logging.LoggerFactory;
//import io.vertx.ext.auth.AuthProvider;
//import io.vertx.ext.auth.User;
//import io.vertx.ext.auth.jwt.JWTAuth;
//import io.vertx.ext.auth.jwt.JWTOptions;
//
///**
// * Mesh auth provider
// */
//@Singleton
//public class MeshAuthProvider implements AuthProvider, JWTAuth {
//
//	private static final Logger log = LoggerFactory.getLogger(MeshAuthProvider.class);
//
//	protected Database db;
//
//	private BCryptPasswordEncoder passwordEncoder;
//
//	@Inject
//	public MeshAuthProvider(BCryptPasswordEncoder passwordEncoder, Database database) {
//		this.passwordEncoder = passwordEncoder;
//		this.db = database;
//	}
//
//	@Override
//	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
//		operateNoTx(() -> {
//			String username = authInfo.getString("username");
//			String password = authInfo.getString("password");
//			MeshAuthUser user = MeshInternal.get().boot().userRoot().findMeshAuthUserByUsername(username);
//			if (user != null) {
//				String accountPasswordHash = user.getPasswordHash();
//				// TODO check if user is enabled
//				boolean hashMatches = false;
//				if (StringUtils.isEmpty(accountPasswordHash) && password != null) {
//					if (log.isDebugEnabled()) {
//						log.debug("The account password hash or token password string are invalid.");
//					}
//					resultHandler.handle(Future.failedFuture(new VertxException("Invalid credentials!")));
//				} else {
//					if (log.isDebugEnabled()) {
//						log.debug("Validating password using the bcrypt password encoder");
//					}
//					hashMatches = passwordEncoder.matches(password, accountPasswordHash);
//				}
//				if (hashMatches) {
//					resultHandler.handle(Future.succeededFuture(user));
//				} else {
//					resultHandler.handle(Future.failedFuture(new VertxException("Invalid credentials!")));
//				}
//			} else {
//				if (log.isDebugEnabled()) {
//					log.debug("Could not load user with username {" + username + "}.");
//				}
//				// TODO Don't let the user know that we know that he did not exist?
//				resultHandler.handle(Future.failedFuture(new VertxException("Invalid credentials!")));
//			}
//
//			//			 , rh -> {
//			//					if (rh.failed()) {
//			//						log.error("Error while authenticating user.", rh.cause());
//			//						resultHandler.handle(Future.failedFuture(rh.cause()));
//			//					}
//			//				});
//			return null;
//		}).subscribe();
//
//	}
//
//	@Override
//	public String generateToken(JsonObject claims, JWTOptions options) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//}
