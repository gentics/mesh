package com.gentics.mesh.core.endpoint.role;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import com.gentics.mesh.error.MissingPermissionException;
import org.apache.commons.lang3.BooleanUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for /api/v1/roles endpoint crud operations.
 */
public class RoleCrudHandlerImpl extends AbstractCrudHandler<HibRole, RoleResponse> implements RoleCrudHandler {

	private static final Logger log = LoggerFactory.getLogger(RoleCrudHandlerImpl.class);

	private BootstrapInitializer boot;

	@Inject
	public RoleCrudHandlerImpl(Database db, BootstrapInitializer boot, HandlerUtilities utils, WriteLock writeLock, RoleDAOActions roleActions) {
		super(db, utils, writeLock, roleActions);
		this.boot = boot;
	}

	/**
	 * Handle a permission read request.
	 * 
	 * @param ac
	 * @param roleUuid
	 *            Uuid of the role which should be used to load the permissions
	 * @param pathToElement
	 *            Path to the element for which the permission should be loaded.
	 */
	public void handlePermissionRead(InternalActionContext ac, String roleUuid, String pathToElement) {
		if (isEmpty(roleUuid)) {
			throw error(BAD_REQUEST, "error_uuid_must_be_specified");
		}
		if (isEmpty(pathToElement)) {
			throw error(BAD_REQUEST, "role_permission_path_missing");
		}

		utils.syncTx(ac, tx -> {
			RoleDao roleDao = tx.roleDao();
			if (log.isDebugEnabled()) {
				log.debug("Handling permission request for element on path {" + pathToElement + "}");
			}
			// 1. Load the role that should be used - read perm implies that the user is able to read the attached permissions
			HibRole role = roleDao.loadObjectByUuid(ac, roleUuid, READ_PERM);

			// 2. Resolve the path to element that is targeted
			HibBaseElement targetElement = boot.rootResolver().resolvePathToElement(pathToElement);
			if (targetElement == null) {
				throw error(NOT_FOUND, "error_element_for_path_not_found", pathToElement);
			}
			RolePermissionResponse response = new RolePermissionResponse();

			// 1. Add granted permissions
			for (InternalPermission perm : roleDao.getPermissions(role, targetElement)) {
				response.set(perm.getRestPerm(), true);
			}
			// 2. Add not granted permissions
			response.setOthers(false);
			return response;
		}, model -> ac.send(model, OK));

	}

	/**
	 * Handle a permission update request.
	 * 
	 * @param ac
	 * @param roleUuid
	 *            Uuid of the role
	 * @param pathToElement
	 *            Path to the element for which the permissions should be updated
	 */
	public void handlePermissionUpdate(InternalActionContext ac, String roleUuid, String pathToElement) {
		if (isEmpty(roleUuid)) {
			throw error(BAD_REQUEST, "error_uuid_must_be_specified");
		}
		if (isEmpty(pathToElement)) {
			throw error(BAD_REQUEST, "role_permission_path_missing");
		}

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, (batch, tx) -> {

				if (log.isDebugEnabled()) {
					log.debug("Handling permission request for element on path {" + pathToElement + "}");
				}

				RoleDao roleDao = tx.roleDao();
				// 1. Load the role that should be used
				HibRole role = roleDao.loadObjectByUuid(ac, roleUuid, UPDATE_PERM);

				// 2. Resolve the path to element that is targeted
				HibBaseElement element = boot.rootResolver().resolvePathToElement(pathToElement);

				if (element == null) {
					throw error(NOT_FOUND, "error_element_for_path_not_found", pathToElement);
				}
				RolePermissionRequest requestModel = ac.fromJson(RolePermissionRequest.class);

				// Prepare the sets for revoke and grant actions
				Set<InternalPermission> permissionsToGrant = new HashSet<>();
				Set<InternalPermission> permissionsToRevoke = new HashSet<>();

				for (InternalPermission permission : InternalPermission.values()) {
					Boolean permValue = requestModel.getPermissions().getNullable(permission.getRestPerm());
					if (permValue != null) {
						if (permValue) {
							permissionsToGrant.add(permission);
						} else {
							permissionsToRevoke.add(permission);
						}
					}
				}
				if (log.isDebugEnabled()) {
					for (InternalPermission p : permissionsToGrant) {
						log.debug("Granting permission: " + p);
					}
					for (InternalPermission p : permissionsToRevoke) {
						log.debug("Revoking permission: " + p);
					}
				}
				// 3. Apply the permission actions
				try {
					roleDao.applyPermissions(ac.getMeshAuthUser(), element, batch, role, BooleanUtils.isTrue(requestModel.getRecursive()), permissionsToGrant,
							permissionsToRevoke);
					String name = role.getName();
					if (ac.getSecurityLogger().isInfoEnabled()) {
						ac.getSecurityLogger().info(String.format("Permission for role {%s} (%s) to {%s} set to %s",
								role.getName(), roleUuid, pathToElement, requestModel.toJson(false)));
					}
					return message(ac, "role_updated_permission", name);
				} catch (MissingPermissionException e) {
					throw error(FORBIDDEN, "error_missing_perm", e.getElementUuid(), e.getPermission().toString());
				}
			}, model -> ac.send(model, OK));
		}
	}

}
