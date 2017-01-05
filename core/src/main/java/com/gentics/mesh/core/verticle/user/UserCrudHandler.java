package com.gentics.mesh.core.verticle.user;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserTokenResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.TokenUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

/**
 * Handler which contains methods for user related requests.
 */
@Singleton
public class UserCrudHandler extends AbstractCrudHandler<User, UserResponse> {

	private static final Logger log = LoggerFactory.getLogger(UserCrudHandler.class);

	private BootstrapInitializer boot;

	@Inject
	public UserCrudHandler(Database db, BootstrapInitializer boot, HandlerUtilities utils) {
		super(db, utils);
		this.boot = boot;
	}

	@Override
	public RootVertex<User> getRootVertex(InternalActionContext ac) {
		return boot.userRoot();
	}

	/**
	 * Handle a permission read request.
	 * 
	 * @param ac
	 * @param userUuid
	 * @param pathToElement
	 */
	public void handlePermissionRead(InternalActionContext ac, String userUuid, String pathToElement) {
		validateParameter(userUuid, "error_uuid_must_be_specified");
		validateParameter(pathToElement, "user_permission_path_missing");

		if (log.isDebugEnabled()) {
			log.debug("Handling permission request for element on path {" + pathToElement + "}");
		}
		db.operateNoTx(() -> {
			// 1. Load the user that should be used - read perm implies that the user is able to read the attached permissions
			User user = boot.userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);

			// 2. Resolve the path to element that is targeted
			MeshVertex targetElement = MeshInternal.get().boot().meshRoot().resolvePathToElement(pathToElement);
			return db.noTx(() -> {
				if (targetElement == null) {
					throw error(NOT_FOUND, "error_element_for_path_not_found", pathToElement);
				}
				UserPermissionResponse response = new UserPermissionResponse();
				for (GraphPermission perm : user.getPermissions(targetElement)) {
					response.getPermissions().add(perm.getSimpleName());
				}
				return Single.just(response);
			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	/**
	 * Handle the fetch token action for the user with the given uuid.
	 * 
	 * @param ac
	 * @param uuid
	 *            User uuid
	 */
	public void handleFetchToken(InternalActionContext ac, String userUuid) {
		validateParameter(userUuid, "The userUuid must not be empty");

		db.operateNoTx(() -> {
			// 1. Load the user that should be used 
			User user = boot.userRoot().loadObjectByUuid(ac, userUuid, CREATE_PERM);
			return db.noTx(() -> {
				String token = TokenUtil.randomToken();
				user.setResetToken(token);
				UserTokenResponse response = new UserTokenResponse();
				response.setToken(token);
				return Single.just(response);
			});
		}).subscribe(model -> ac.send(model, CREATED), ac::fail);
	}

}
