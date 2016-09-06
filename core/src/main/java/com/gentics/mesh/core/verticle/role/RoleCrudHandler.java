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

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
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
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

public class RoleCrudHandler extends AbstractCrudHandler<Role, RoleResponse> {

	private static final Logger log = LoggerFactory.getLogger(RoleCrudHandler.class);

	private BootstrapInitializer boot;

	@Inject
	public RoleCrudHandler(Database db, BootstrapInitializer boot) {
		super(db);
		this.boot = boot;
	}

	@Override
	public RootVertex<Role> getRootVertex(InternalActionContext ac) {
		return boot.roleRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		HandlerUtilities.deleteElement(ac, () -> getRootVertex(ac), uuid);
	}

	/**
	 * Handle a permissions read request.
	 * 
	 * @param ac
	 * @param roleUuid
	 *            Uuid of the role which should be used to load the permissions
	 * @param pathToElement
	 *            Path to the element for which the permissions should be loaded.
	 */
	public void handlePermissionRead(InternalActionContext ac, String roleUuid, String pathToElement) {
		if (isEmpty(roleUuid)) {
			throw error(BAD_REQUEST, "error_uuid_must_be_specified");
		}
		if (isEmpty(pathToElement)) {
			throw error(BAD_REQUEST, "role_permission_path_missing");
		}

		db.asyncNoTx(() -> {

			if (log.isDebugEnabled()) {
				log.debug("Handling permissions request for element on path {" + pathToElement + "}");
			}
			// 1. Load the role that should be used - read perm implies that the user is able to read the attached permissions
			return boot.roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM).flatMap(role -> {
				return db.noTx(() -> {
					// 2. Resolve the path to element that is targeted
					return MeshRoot.getInstance().resolvePathToElement(pathToElement).flatMap(targetElement -> {
						if (targetElement == null) {
							throw error(NOT_FOUND, "error_element_for_path_not_found", pathToElement);
						}
						RolePermissionResponse response = new RolePermissionResponse();
						for (GraphPermission perm : role.getPermissions(targetElement)) {
							response.getPermissions().add(perm.getSimpleName());
						}
						return Single.just(response);

					});
				});
			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);

	}

	/**
	 * Handle a permissions update request.
	 * 
	 * @param ac
	 * @param roleUuid
	 *            Uuid of the role
	 * @param pathToElement
	 *            Path to the element for which the permissions should be updated
	 */
	public void handlePermissionUpdate(InternalActionContext ac, String roleUuid, String pathToElement) {
		db.asyncNoTx(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Handling permissions request for element on path {" + pathToElement + "}");
			}
			if (isEmpty(roleUuid)) {
				throw error(BAD_REQUEST, "error_uuid_must_be_specified");
			}

			if (isEmpty(pathToElement)) {
				throw error(BAD_REQUEST, "role_permission_path_missing");
			}

			// 1. Load the role that should be used
			Single<Role> obsRole = boot.roleRoot().loadObjectByUuid(ac, roleUuid, UPDATE_PERM);

			// 2. Resolve the path to element that is targeted
			Single<? extends MeshVertex> obsElement = MeshRoot.getInstance().resolvePathToElement(pathToElement);

			return Single.zip(obsRole, obsElement, (role, element) -> {

				if (element == null) {
					throw error(NOT_FOUND, "error_element_for_path_not_found", pathToElement);
				}

				return db.noTx(() -> {
					RolePermissionRequest requestModel = ac.fromJson(RolePermissionRequest.class);

					// Prepare the sets for revoke and grant actions
					Role updatedRole = db.tx(() -> {
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
								log.debug("Adding permissions {" + permission.getSimpleName() + "} to list of permissions to add.");
							}
							permissionsToRevoke.remove(permission);
							permissionsToGrant.add(permission);
						}
						if (log.isDebugEnabled()) {
							for (GraphPermission p : permissionsToGrant) {
								log.debug("Granting permissions: " + p);
							}
							for (GraphPermission p : permissionsToRevoke) {
								log.debug("Revoking permissions: " + p);
							}
						}

						// 3. Apply the permissions actions
						element.applyPermissions(role, BooleanUtils.isTrue(requestModel.getRecursive()), permissionsToGrant, permissionsToRevoke);
						return role;
					});

					return Single.just(message(ac, "role_updated_permission", updatedRole.getName()));

				});
			}).flatMap(x -> x);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}
}
