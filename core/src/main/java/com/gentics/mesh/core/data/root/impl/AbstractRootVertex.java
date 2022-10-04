package com.gentics.mesh.core.data.root.impl;

import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;

/**
 * Abstract implementation for root vertices which are aggregation vertices for mesh core vertices. The abstract implementation contains various helper methods
 * that are useful for loading lists and items from the root vertex.
 * 
 * @see RootVertex
 * @param <T>
 */
public abstract class AbstractRootVertex<T extends MeshCoreVertex<? extends RestModel, T>> extends MeshVertexImpl implements RootVertex<T> {

	@Override
	abstract public Class<? extends T> getPersistanceClass();

	@Override
	abstract public String getRootLabel();

	@Override
	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return mesh().permissionProperties().getRolePermissions(this, ac, roleUuid);
	}

	@Override
	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return mesh().permissionProperties().getRolesWithPerm(this, perm);
	}

	@Override
	public boolean applyPermissions(MeshAuthUser user, EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
									Set<GraphPermission> permissionsToRevoke) {
		boolean permissionChanged = false;
		if (recursive) {
			for (T t : findAll().stream().filter(e -> user.hasPermission(e, GraphPermission.READ_PERM)).collect(Collectors.toList())) {
				permissionChanged = t.applyPermissions(user, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}
		permissionChanged = applyVertexPermissions(user, batch, role, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;
	}
}
