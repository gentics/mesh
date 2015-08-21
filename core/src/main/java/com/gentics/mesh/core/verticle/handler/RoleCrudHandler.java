package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.fail;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.responde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

@Component
public class RoleCrudHandler extends AbstractCrudHandler {

	private static final Logger log = LoggerFactory.getLogger(RoleCrudHandler.class);

	@Override
	public void handleCreate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			createObject(rc, boot.roleRoot());
		}
	}

	@Override
	public void handleDelete(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			deleteObject(rc, "uuid", "role_deleted", boot.roleRoot());
		}
	}

	@Override
	public void handleRead(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadTransformAndResponde(rc, "uuid", READ_PERM, boot.roleRoot());
		}
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			updateObject(rc, "uuid", boot.roleRoot());
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadTransformAndResponde(rc, boot.roleRoot(), new RoleListResponse());
		}
	}

	public void handlePermissionUpdate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			String roleUuid = rc.request().getParam("param0");
			String pathToElement = rc.request().params().get("param1");
			if (StringUtils.isEmpty(roleUuid)) {
				fail(rc, "error_uuid_must_be_specified");
			} else if (StringUtils.isEmpty(pathToElement)) {
				fail(rc, "role_permission_path_missing");
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Handling permission request for element on path {" + pathToElement + "}");
				}
				// 1. Load the role that should be used
				loadObjectByUuid(rc, roleUuid, UPDATE_PERM, boot.roleRoot(), rh -> {
					if (hasSucceeded(rc, rh)) {
						Role role = rh.result();
						RolePermissionRequest requestModel = fromJson(rc, RolePermissionRequest.class);
						//2. Resolve the path to element that is targeted
						MeshRoot.getInstance().resolvePathToElement(pathToElement, vertex -> {
							if (hasSucceeded(rc, vertex)) {
								MeshVertex targetElement = vertex.result();

								// Prepare the sets for revoke and grant actions
								try (Trx txUpdate = new Trx(db)) {
									Set<GraphPermission> permissionsToGrant = new HashSet<>();
									Set<GraphPermission> permissionsToRevoke = new HashSet<>();
									permissionsToRevoke.add(CREATE_PERM);
									permissionsToRevoke.add(READ_PERM);
									permissionsToRevoke.add(UPDATE_PERM);
									permissionsToRevoke.add(DELETE_PERM);
									for (String permName : requestModel.getPermissions()) {
										GraphPermission permission = GraphPermission.valueOfSimpleName(permName);
										if (permission == null) {
											txUpdate.failure();
											fail(rc, "role_error_permission_name_unknown", permName);
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
									targetElement.applyPermissions(role, BooleanUtils.isTrue(requestModel.getRecursive()), permissionsToGrant,
											permissionsToRevoke);
									txUpdate.success();
								}
								responde(rc, toJson(new GenericMessageResponse(i18n.get(rc, "role_updated_permission", role.getName()))));
							}
						});

					}
				});
			}
		}
	}
}
