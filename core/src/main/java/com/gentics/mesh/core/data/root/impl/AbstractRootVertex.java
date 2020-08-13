package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.ETag;

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
	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			for (T t : findAll()) {
				t.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
			}
		}
		applyVertexPermissions(batch, role, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public PermissionInfo getRolePermissions(MeshVertex vertex, InternalActionContext ac, String roleUuid) {
		return mesh().permissionProperties().getRolePermissions(vertex, ac, roleUuid);
	}

	@Override
	public TraversalResult<? extends Role> getRolesWithPerm(MeshVertex vertex, GraphPermission perm) {
		return mesh().permissionProperties().getRolesWithPerm(vertex, perm);
	}

	public void setRolePermissions(MeshVertex vertex, InternalActionContext ac, GenericRestResponse model) {
		model.setRolePerms(getRolePermissions(vertex, ac, ac.getRolePermissionParameters().getRoleUuid()));
	}

	public String getAPIPath(T element, InternalActionContext ac) {
		// TODO FIXME remove this method, must be implemented in all derived classes
		throw new RuntimeException("Not implemented");
	}

	public RestModel transformToRestSync(T element, InternalActionContext ac, int level, String... languageTags) {
		// TODO FIXME remove this method, must be implemented in all derived classes
		throw new RuntimeException("Not implemented");
	}

	public final String getETag(T element, InternalActionContext ac) {
		UserDaoWrapper userDao = mesh().boot().userDao();
		RoleDaoWrapper roleDao = mesh().boot().roleDao();

		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(getUuid());
		keyBuilder.append("-");
		keyBuilder.append(userDao.getPermissionInfo(ac.getUser(), element).getHash());

		keyBuilder.append("fields:");
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();
		fields.forEach(keyBuilder::append);

		/**
		 * permissions (&roleUuid query parameter aware)
		 *
		 * Permissions can change and thus must be included in the etag computation in order to invalidate the etag once the permissions change.
		 */
		String roleUuid = ac.getRolePermissionParameters().getRoleUuid();
		if (!isEmpty(roleUuid)) {
			Role role = mesh().boot().meshRoot().getRoleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);
			if (role != null) {
				Set<GraphPermission> permSet = roleDao.getPermissions(role, element);
				Set<String> humanNames = new HashSet<>();
				for (GraphPermission permission : permSet) {
					humanNames.add(permission.getRestPerm().getName());
				}
				String[] names = humanNames.toArray(new String[humanNames.size()]);
				keyBuilder.append(Arrays.toString(names));
			}

		}

		// Add the type specific etag part
		keyBuilder.append(getSubETag(element, ac));
		return ETag.hash(keyBuilder.toString());
	}

	/**
	 * This method provides the element specific etag. It needs to be individually implemented for all core element classes.
	 *
	 * @param element
	 * @param ac
	 * @return
	 */
	public String getSubETag(T element, InternalActionContext ac) {
		// TODO FIXME make this method abstract
		throw new RuntimeException("Not implemented");
	}
}
