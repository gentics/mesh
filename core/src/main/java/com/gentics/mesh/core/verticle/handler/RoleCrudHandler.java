package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.VerticleHelper.fail;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.responde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

@Component
public class RoleCrudHandler extends AbstractCrudHandler {

	private static final Logger log = LoggerFactory.getLogger(RoleCrudHandler.class);

	@Override
	public void handleCreate(RoutingContext rc) {
		RoleCreateRequest requestModel = fromJson(rc, RoleCreateRequest.class);
		MeshAuthUser requestUser = getUser(rc);
		if (StringUtils.isEmpty(requestModel.getName())) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_name_must_be_set")));
			return;
		}

		if (StringUtils.isEmpty(requestModel.getGroupUuid())) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "role_missing_parentgroup_field")));
			return;
		}

		try (Trx tx = new Trx(db)) {
			if (boot.roleRoot().findByName(requestModel.getName()) != null) {
				rc.fail(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "role_conflicting_name")));
				return;
			}
			loadObjectByUuid(rc, requestModel.getGroupUuid(), CREATE_PERM, boot.groupRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Group parentGroup = rh.result();

					Role role = boot.roleRoot().create(requestModel.getName(), parentGroup, requestUser);
					requestUser.addCRUDPermissionOnRole(parentGroup, CREATE_PERM, role);
					tx.success();
					searchQueue().put(role.getUuid(), Role.TYPE, SearchQueueEntryAction.CREATE_ACTION);
					vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
					transformAndResponde(rc, role);
				}
			});
		}
	}

	@Override
	public void handleDelete(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			delete(rc, "uuid", "role_deleted", boot.roleRoot());
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

			loadObject(rc, "uuid", UPDATE_PERM, boot.roleRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Role role = rh.result();
					RoleUpdateRequest requestModel = fromJson(rc, RoleUpdateRequest.class);

					if (!StringUtils.isEmpty(requestModel.getName()) && role.getName() != requestModel.getName()) {
						if (boot.roleRoot().findByName(requestModel.getName()) != null) {
							rc.fail(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "role_conflicting_name")));
							return;
						}
						role.setName(requestModel.getName());
					}
					searchQueue().put(role.getUuid(), Role.TYPE, SearchQueueEntryAction.UPDATE_ACTION);
					vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
					transformAndResponde(rc, role);
				}
			});
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
