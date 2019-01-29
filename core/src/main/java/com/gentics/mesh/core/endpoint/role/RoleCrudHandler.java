package com.gentics.mesh.core.endpoint.role;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
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
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.EventQueueBatch;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.reactivex.Single;

public class RoleCrudHandler extends AbstractCrudHandler<Role, RoleResponse> {

	private static final Logger log = LoggerFactory.getLogger(RoleCrudHandler.class);

	private BootstrapInitializer boot;

	private SearchQueue searchQueue;

	@Inject
	public RoleCrudHandler(Database db, BootstrapInitializer boot, HandlerUtilities utils, SearchQueue searchQueue) {
		super(db, utils);
		this.boot = boot;
		this.searchQueue = searchQueue;
	}

	@Override
	public RootVertex<Role> getRootVertex(InternalActionContext ac) {
		return boot.roleRoot();
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

		db.asyncTx(() -> {

			if (log.isDebugEnabled()) {
				log.debug("Handling permission request for element on path {" + pathToElement + "}");
			}
			// 1. Load the role that should be used - read perm implies that the user is able to read the attached permissions
			Role role = boot.roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);

			// 2. Resolve the path to element that is targeted
			MeshVertex targetElement = MeshInternal.get().boot().meshRoot().resolvePathToElement(pathToElement);
			if (targetElement == null) {
				throw error(NOT_FOUND, "error_element_for_path_not_found", pathToElement);
			}
			RolePermissionResponse response = new RolePermissionResponse();

			// 1. Add granted permissions
			for (GraphPermission perm : role.getPermissions(targetElement)) {
				response.set(perm.getRestPerm(), true);
			}
			// 2. Add not granted permissions
			response.setOthers(false);
			return Single.just(response);
		}).subscribe(model -> ac.send(model, OK), ac::fail);

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

		db.asyncTx(() -> {
			if (log.isDebugEnabled()) {
				log.debug("Handling permission request for element on path {" + pathToElement + "}");
			}

			// 1. Load the role that should be used
			Role role = boot.roleRoot().loadObjectByUuid(ac, roleUuid, UPDATE_PERM);

			// 2. Resolve the path to element that is targeted
			MeshVertex element = MeshInternal.get().boot().meshRoot().resolvePathToElement(pathToElement);

			if (element == null) {
				throw error(NOT_FOUND, "error_element_for_path_not_found", pathToElement);
			}

			return db.tx(() -> {
				RolePermissionRequest requestModel = ac.fromJson(RolePermissionRequest.class);

				// Prepare the sets for revoke and grant actions
				Tuple<EventQueueBatch, String> tuple = db.tx(() -> {
					EventQueueBatch batch = searchQueue.create();
					Set<GraphPermission> permissionsToGrant = new HashSet<>();
					Set<GraphPermission> permissionsToRevoke = new HashSet<>();

					for (GraphPermission permission : GraphPermission.values()) {

						if (requestModel.getPermissions().get(permission.getRestPerm()) == true) {
							permissionsToGrant.add(permission);
						} else {
							permissionsToRevoke.add(permission);
						}
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
					element.applyPermissions(batch, role, BooleanUtils.isTrue(requestModel.getRecursive()), permissionsToGrant, permissionsToRevoke);
					return Tuple.tuple(batch, role.getName());
				});

				tuple.v1().processSync();
				String name = tuple.v2();
				return Single.just(message(ac, "role_updated_permission", name));

			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}
}
