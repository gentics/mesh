package com.gentics.mesh.core.verticle.role;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.GenericMessageResponse.message;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

@Component
public class RoleCrudHandler extends AbstractCrudHandler<Role, RoleResponse> {

	private static final Logger log = LoggerFactory.getLogger(RoleCrudHandler.class);

	@Override
	public RootVertex<Role> getRootVertex(InternalActionContext ac) {
		return boot.roleRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "role_deleted");
	}

	public void handlePermissionRead(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			if (StringUtils.isEmpty(roleUuid)) {
				throw error(BAD_REQUEST, "error_uuid_must_be_specified");
			}

			if (StringUtils.isEmpty(pathToElement)) {
				throw error(BAD_REQUEST, "role_permission_path_missing");
			}
			if (log.isDebugEnabled()) {
				log.debug("Handling permission request for element on path {" + pathToElement + "}");
			}
			// 1. Load the role that should be used - read perm implies that the user is able to read the attached permissions
			return boot.roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM).flatMap(role -> {
				return db.noTrx(() -> {
					// 2. Resolve the path to element that is targeted
					return MeshRoot.getInstance().resolvePathToElement(pathToElement).flatMap(targetElement -> {
						if (targetElement == null) {
							throw error(NOT_FOUND, "error_element_for_path_not_found", pathToElement);
						}
						RolePermissionResponse response = new RolePermissionResponse();
						for (GraphPermission perm : role.getPermissions(targetElement)) {
							response.getPermissions().add(perm.getSimpleName());
						}
						return Observable.just(response);

					});
				});
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

	public void handlePermissionUpdate(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			if (log.isDebugEnabled()) {
				log.debug("Handling permission request for element on path {" + pathToElement + "}");
			}
			if (isEmpty(roleUuid)) {
				throw error(BAD_REQUEST, "error_uuid_must_be_specified");
			}

			if (isEmpty(pathToElement)) {
				throw error(BAD_REQUEST, "role_permission_path_missing");
			}

			// 1. Load the role that should be used
			Observable<Role> obsRole = boot.roleRoot().loadObjectByUuid(ac, roleUuid, UPDATE_PERM);
			// 2. Resolve the path to element that is targeted
			Observable<? extends MeshVertex> obsElement = MeshRoot.getInstance().resolvePathToElement(pathToElement);

			return Observable.zip(obsRole, obsElement, (role, element) -> {

				if (element == null) {
					throw error(NOT_FOUND, "error_element_for_path_not_found", pathToElement);
				}

				return db.noTrx(() -> {
					RolePermissionRequest requestModel = ac.fromJson(RolePermissionRequest.class);

					// Prepare the sets for revoke and grant actions
					Role updatedRole = db.trx(() -> {
						Set<GraphPermission> permissionsToGrant = new HashSet<>();
						Set<GraphPermission> permissionsToRevoke = new HashSet<>();
						permissionsToRevoke.add(CREATE_PERM);
						permissionsToRevoke.add(READ_PERM);
						permissionsToRevoke.add(UPDATE_PERM);
						permissionsToRevoke.add(DELETE_PERM);
						for (String permName : requestModel.getPermissions()) {
							GraphPermission permission = GraphPermission.valueOfSimpleName(permName);
							if (permission == null) {
								throw error(BAD_REQUEST, "role_error_permission_name_unknown", permName);
							}
							if (log.isDebugEnabled()) {
								log.debug("Adding permission {" + permission.getSimpleName() + "} to list of permissions to add.");
							}
							permissionsToRevoke.remove(permission);
							permissionsToGrant.add(permission);
						}
						if (log.isDebugEnabled()) {
							for (GraphPermission p : permissionsToGrant) {
								log.debug("Granting permission: " + p);
							}
							for (GraphPermission p : permissionsToRevoke) {
								log.debug("Revoking permission: " + p);
							}
						}

						// 3. Apply the permission actions
						element.applyPermissions(role, BooleanUtils.isTrue(requestModel.getRecursive()), permissionsToGrant, permissionsToRevoke);
						return role;
					});

					return Observable.just(message(ac, "role_updated_permission", updatedRole.getName()));

				});
			}).flatMap(x -> x);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}
}
