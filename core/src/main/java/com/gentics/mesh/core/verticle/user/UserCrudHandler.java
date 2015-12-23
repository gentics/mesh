package com.gentics.mesh.core.verticle.user;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

@Component
public class UserCrudHandler extends AbstractCrudHandler<User, UserResponse> {

	private static final Logger log = LoggerFactory.getLogger(UserCrudHandler.class);

	@Override
	public RootVertex<User> getRootVertex(InternalActionContext ac) {
		return boot.userRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "user_deleted");
	}

	public void handlePermissionRead(InternalActionContext ac) {
		db.asyncNoTrx(() -> {
			String userUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			if (StringUtils.isEmpty(userUuid)) {
				throw error(BAD_REQUEST, "error_uuid_must_be_specified");
			}

			if (StringUtils.isEmpty(pathToElement)) {
				throw error(BAD_REQUEST, "user_permission_path_missing");
			}

			if (log.isDebugEnabled()) {
				log.debug("Handling permission request for element on path {" + pathToElement + "}");
			}

			return db.noTrx(() -> {
				// 1. Load the role that should be used - read perm implies that the user is able to read the attached permissions
				Observable<User> obsUser = boot.userRoot().loadObjectByUuid(ac, userUuid, READ_PERM);

				// 2. Resolve the path to element that is targeted
				Observable<? extends MeshVertex> resolvedElement = MeshRoot.getInstance().resolvePathToElement(pathToElement);

				Observable<UserPermissionResponse> respObs = Observable.zip(obsUser, resolvedElement, (user, targetElement) -> {

					return db.noTrx(() -> {
						if (targetElement == null) {
							throw error(NOT_FOUND, "error_element_for_path_not_found", pathToElement);
						}
						UserPermissionResponse response = new UserPermissionResponse();
						for (GraphPermission perm : user.getPermissions(ac, targetElement)) {
							response.getPermissions().add(perm.getSimpleName());
						}
						return response;
					});
				});
				return respObs;
			}).toBlocking().first();
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

}
