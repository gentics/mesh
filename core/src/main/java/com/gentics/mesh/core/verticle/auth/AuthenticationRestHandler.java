package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

@Singleton
public class AuthenticationRestHandler extends AbstractHandler {

	protected Database db;

	public static final String TOKEN_COOKIE_KEY = "mesh.token";

	@Inject
	public AuthenticationRestHandler(Database db) {
		this.db = db;
	}

	/**
	 * Handle a <code>/me</code> request which will return the current user as a JSON response.
	 * 
	 * @param ac
	 */
	public void handleMe(InternalActionContext ac) {
		operateNoTx(() -> {
			//TODO add permission check
			MeshAuthUser requestUser = ac.getUser();
			return requestUser.transformToRest(ac, 0);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle a logout request.
	 * 
	 * @param ac
	 */
	public void handleLogout(InternalActionContext ac) {
		ac.logout();
		GenericMessageResponse message = new GenericMessageResponse("OK");
		ac.send(JsonUtil.toJson(message), OK);
	}

//	/**
//	 * Handle a login request.
//	 * 
//	 * @param ac
//	 */
//	/**
//	 * Handle a login request.
//	 * 
//	 * @param ac
//	 */
//	public void handleLogin(InternalActionContext ac) {
//		try {
//			LoginRequest request = JsonUtil.readValue(ac.getBodyAsString(), LoginRequest.class);
//			// TODO fail on missing field
//			JsonObject authInfo = new JsonObject().put("username", request.getUsername()).put("password", request.getPassword());
//			authProvider.authenticate(authInfo, rh -> {
//				if (rh.failed()) {
//					throw error(UNAUTHORIZED, "auth_login_failed", rh.cause());
//				} else {
//					User user = rh.result();
//					if (user instanceof MeshAuthUser) {
//						ac.setUser((MeshAuthUser) user);
//						ac.send(JsonUtil.toJson(new GenericMessageResponse("OK")), OK);
//					} else {
//						log.error("Auth Provider did not return a {" + MeshAuthUser.class.getName() + "} user got {" + user.getClass().getName()
//								+ "} instead.");
//						throw error(BAD_REQUEST, "auth_login_failed");
//					}
//				}
//			});
//		} catch (Exception e) {
//			throw error(UNAUTHORIZED, "auth_login_failed", e);
//		}
//
//	}

//	public void handleLoginJWT(InternalActionContext ac) {
//		MeshJWTAuthProvider provider = getAuthProvider();
//
//		try {
//			LoginRequest request = JsonUtil.readValue(ac.getBodyAsString(), LoginRequest.class);
//			if (request.getUsername() == null) {
//				throw error(BAD_REQUEST, "error_json_field_missing", "username");
//			}
//			if (request.getPassword() == null) {
//				throw error(BAD_REQUEST, "error_json_field_missing", "password");
//			}
//
//			provider.generateToken(request.getUsername(), request.getPassword(), rh -> {
//				if (rh.failed()) {
//					throw error(UNAUTHORIZED, "auth_login_failed", rh.cause());
//				} else {
//					ac.addCookie(Cookie.cookie(TOKEN_COOKIE_KEY, rh.result()).setPath("/"));
//					ac.send(JsonUtil.toJson(new TokenResponse(rh.result())));
//				}
//			});
//		} catch (Exception e) {
//			throw error(UNAUTHORIZED, "auth_login_failed", e);
//		}
//	}

//	/**
//	 * Gets the auth provider as MeshJWTAuthProvider
//	 * 
//	 * @return
//	 */
//	private MeshJWTAuthProvider getAuthProvider() {
//		if (provider instanceof MeshJWTAuthProvider) {
//			return (MeshJWTAuthProvider) provider;
//		} else {
//			throw new IllegalStateException("AuthProvider must be an instance of MeshJWTAuthProvider when using JWT! Got {" + provider + "}");
//		}
//	}

}
