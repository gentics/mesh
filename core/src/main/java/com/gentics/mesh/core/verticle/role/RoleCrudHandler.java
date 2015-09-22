package com.gentics.mesh.core.verticle.role;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.VerticleHelper.createObject;
import static com.gentics.mesh.util.VerticleHelper.deleteObject;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.updateObject;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

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
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public class RoleCrudHandler extends AbstractCrudHandler {

	private static final Logger log = LoggerFactory.getLogger(RoleCrudHandler.class);

	@Override
	public void handleCreate(InternalActionContext ac) {
		try (NoTrx tx = db.noTrx()) {
			createObject(ac, boot.roleRoot());
		}
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		try (NoTrx tx = db.noTrx()) {
			deleteObject(ac, "uuid", "role_deleted", boot.roleRoot());
		}
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		//		Mesh.vertx().executeBlocking(bc -> {
		try (NoTrx tx = db.noTrx()) {
			loadTransformAndResponde(ac, "uuid", READ_PERM, boot.roleRoot());
		}
		//		} , false, rh -> {
		//			if (rh.failed()) {
		//				rc.fail(rh.cause());
		//			}
		//		});
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		try (NoTrx tx = db.noTrx()) {
			updateObject(ac, "uuid", boot.roleRoot());
		}
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		try (NoTrx tx = db.noTrx()) {
			loadTransformAndResponde(ac, boot.roleRoot(), new RoleListResponse());
		}
	}

	public void handlePermissionUpdate(InternalActionContext ac) {
		try (NoTrx tx = db.noTrx()) {
			String roleUuid = ac.getParameter("param0");
			String pathToElement = ac.getParameter("param1");
			if (StringUtils.isEmpty(roleUuid)) {
				ac.fail(BAD_REQUEST, "error_uuid_must_be_specified");
			} else if (StringUtils.isEmpty(pathToElement)) {
				ac.fail(BAD_REQUEST, "role_permission_path_missing");
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Handling permission request for element on path {" + pathToElement + "}");
				}
				// 1. Load the role that should be used
				loadObjectByUuid(ac, roleUuid, UPDATE_PERM, boot.roleRoot(), rh -> {
					if (hasSucceeded(ac, rh)) {
						Role role = rh.result();
						RolePermissionRequest requestModel = ac.fromJson(RolePermissionRequest.class);
						//2. Resolve the path to element that is targeted
						MeshRoot.getInstance().resolvePathToElement(pathToElement, vertex -> {
							if (hasSucceeded(ac, vertex)) {
								MeshVertex targetElement = vertex.result();

								// Prepare the sets for revoke and grant actions
								try (Trx txUpdate = db.trx()) {
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
											ac.fail(BAD_REQUEST, "role_error_permission_name_unknown", permName);
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
								ac.send(toJson(new GenericMessageResponse(ac.i18n("role_updated_permission", role.getName()))));
							}
						});

					}
				});
			}
		}
	}
}
