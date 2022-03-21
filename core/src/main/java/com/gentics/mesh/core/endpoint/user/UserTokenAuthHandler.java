package com.gentics.mesh.core.endpoint.user;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.handler.MeshJWTAuthHandler;
import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.parameter.UserParameters;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.AuthHandlerImpl;

/**
 * The user token authentication handler grants access to routes by validating the provides token query parameter value.
 * 
 * Please note that is very important to always chain the MeshAuthHandler after this handler because it will just inject a User into the routing context. This
 * handler will also call rc.next() if no token has been provided. The token code is only a fallback mechanism and should not replace the JWT auth handler. If
 * this handler fails the {@link MeshJWTAuthHandler} should try to extract the JWT token from the cookie and load the correct user.
 */
@Singleton
public class UserTokenAuthHandler extends AuthHandlerImpl {

	public static final int DEFAULT_MAX_TOKEN_AGE_IN_MINS = 30;
	private Database db;

	@Inject
	public UserTokenAuthHandler(MeshJWTAuthProvider authProvider, Database db) {
		super(authProvider);
		this.db = db;
	}

	@Override
	public void parseCredentials(RoutingContext context, Handler<AsyncResult<JsonObject>> handler) {
		// Not needed
	}

	@Override
	public void handle(RoutingContext rc) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		UserParameters parameters = ac.getUserParameters();
		String token = parameters.getToken();
		String uuid = ac.getParameter("userUuid");
		if (ac.getUser() == null && !isEmpty(token)) {

			MeshAuthUser lastEditor = db.tx(tx -> {
				// 1. Load the element from the root element using the given uuid
				UserDao userDao = tx.userDao();
				HibUser element = userDao.findByUuid(uuid);

				if (element == null) {
					throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
				}

				// 2. Validate the provided token code
				if (!userDao.isResetTokenValid(element, token, DEFAULT_MAX_TOKEN_AGE_IN_MINS)) {
					return null;
				}

				// TODO maybe the token should only be reset once the user
				// update occurred
				element.invalidateResetToken();

				// TODO it would be better to store the designated token
				// requester instead and use that user
				return MeshAuthUserImpl.create(db, element.getEditor());
			});
			if (lastEditor == null) {
				throw error(UNAUTHORIZED, "user_error_provided_token_invalid");
			}
			rc.setUser(lastEditor);

			// Token found and validated. Lets continue
			rc.next();
		} else {
			// No token could be found. Lets continue with another auth handler
			// which should be added to the original request route.
			rc.next();
		}
	}

}
