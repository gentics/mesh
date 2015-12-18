package com.gentics.mesh.core.verticle.role;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public class RoleCrudHandler extends AbstractCrudHandler {

	private static final Logger log = LoggerFactory.getLogger(RoleCrudHandler.class);

	@Override
	public void handleCreate(InternalActionContext ac) {
		createElement(ac, () -> boot.roleRoot());
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> boot.roleRoot(), "uuid", "role_deleted");
	}

	@Override
	public void handleRead(InternalActionContext ac) {
		readElement(ac, "uuid", () -> boot.roleRoot());
	}

	@Override
	public void handleUpdate(InternalActionContext ac) {
		updateElement(ac, "uuid", () -> boot.roleRoot());
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		readElementList(ac, () -> boot.roleRoot());
	}

	public void handlePermissionRead(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
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
				// 1. Load the role that should be used - read perm implies that the user is able to read the attached permissions
				boot.roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM, rh -> {
					if (hasSucceeded(ac, rh)) {

						db.noTrx(noTx -> {
							// 2. Resolve the path to element that is targeted
							MeshRoot.getInstance().resolvePathToElement(pathToElement, vertex -> {
								if (hasSucceeded(ac, vertex)) {
									if (vertex.result() == null) {
										ac.errorHandler().handle(failedFuture(NOT_FOUND, "error_element_for_path_not_found", pathToElement));
										return;
									}
									MeshVertex targetElement = vertex.result();
									Role role = rh.result();
									RolePermissionResponse response = new RolePermissionResponse();
									for (GraphPermission perm : role.getPermissions(targetElement)) {
										response.getPermissions().add(perm.getSimpleName());
									}
									ac.send(JsonUtil.toJson(response));
								}
							});

						});

					}
				});
			}
		} , ac.errorHandler());
	}

	public void handlePermissionUpdate(InternalActionContext ac) {
		db.asyncNoTrx(tc -> {
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
				boot.roleRoot().loadObjectByUuid(ac, roleUuid, UPDATE_PERM, rh -> {
					if (hasSucceeded(ac, rh)) {

						db.noTrx(noTx -> {
							RolePermissionRequest requestModel = ac.fromJson(RolePermissionRequest.class);
							// 2. Resolve the path to element that is targeted
							MeshRoot.getInstance().resolvePathToElement(pathToElement, vertex -> {
								if (hasSucceeded(ac, vertex)) {
									if (vertex.result() == null) {
										ac.errorHandler().handle(failedFuture(NOT_FOUND, "error_element_for_path_not_found", pathToElement));
										return;
									}
									MeshVertex targetElement = vertex.result();

									// Prepare the sets for revoke and grant actions
									db.trx(txUpdate -> {
										Set<GraphPermission> permissionsToGrant = new HashSet<>();
										Set<GraphPermission> permissionsToRevoke = new HashSet<>();
										permissionsToRevoke.add(CREATE_PERM);
										permissionsToRevoke.add(READ_PERM);
										permissionsToRevoke.add(UPDATE_PERM);
										permissionsToRevoke.add(DELETE_PERM);
										for (String permName : requestModel.getPermissions()) {
											GraphPermission permission = GraphPermission.valueOfSimpleName(permName);
											if (permission == null) {
												txUpdate.fail(new HttpStatusCodeErrorException(BAD_REQUEST,
														ac.i18n("role_error_permission_name_unknown", permName)));
												return;
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
										Role role = rh.result();
										targetElement.applyPermissions(role, BooleanUtils.isTrue(requestModel.getRecursive()), permissionsToGrant,
												permissionsToRevoke);
										txUpdate.complete(role);
									} , (AsyncResult<Role> txUpdated) -> {
										if (txUpdated.failed()) {
											ac.errorHandler().handle(Future.failedFuture(txUpdated.cause()));
										} else {
											Role role = txUpdated.result();
											ac.sendMessage(OK, "role_updated_permission", role.getName());
										}
									});
								}
							});

						});

					}
				});
			}
		} , ac.errorHandler());
	}
}
