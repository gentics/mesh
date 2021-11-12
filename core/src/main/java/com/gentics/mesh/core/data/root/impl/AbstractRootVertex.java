package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
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
public abstract class AbstractRootVertex<T extends MeshCoreVertex<? extends RestModel>> extends MeshVertexImpl implements RootVertex<T> {

	@Override
	abstract public Class<? extends T> getPersistanceClass();

	@Override
	abstract public String getRootLabel();

	@Override
	public boolean applyPermissions(EventQueueBatch batch, HibRole role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
		boolean permissionChanged = false;
		if (recursive) {
			for (T t : findAll()) {
				permissionChanged = t.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}
		permissionChanged = RootVertex.super.applyPermissions(batch, toGraph(role), false, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;
	}

	@Override
	public PermissionInfo getRolePermissions(HibBaseElement element, InternalActionContext ac, String roleUuid) {
		return mesh().boot().roleDao().getRolePermissions(element, ac, roleUuid);
	}

	@Override
	public Result<? extends HibRole> getRolesWithPerm(HibBaseElement vertex, InternalPermission perm) {
		return mesh().boot().roleDao().getRolesWithPerm(vertex, perm);
	}

	/**
	 * Update the role permissions for the given vertex.
	 * 
	 * @param vertex
	 * @param ac
	 * @param model
	 */
	public void setRolePermissions(MeshVertex vertex, InternalActionContext ac, GenericRestResponse model) {
		model.setRolePerms(getRolePermissions(vertex, ac, ac.getRolePermissionParameters().getRoleUuid()));
	}

	/**
	 * Not implemented for abstract implementations.
	 * 
	 * @param element
	 * @param ac
	 * @return
	 */
	public String getAPIPath(T element, InternalActionContext ac) {
		// TODO FIXME remove this method, must be implemented in all derived classes
		throw new RuntimeException("Not implemented");
	}

	/**
	 * Root elements can't be transformed to REST.
	 * 
	 * @param element
	 * @param ac
	 * @param level
	 * @param languageTags
	 * @return
	 */
	public RestModel transformToRestSync(T element, InternalActionContext ac, int level, String... languageTags) {
		// TODO FIXME remove this method, must be implemented in all derived classes
		throw new RuntimeException("Not implemented");
	}

	/**
	 * Generate the eTag for the element. Every time the edges to the root vertex change the internal element version gets updated.
	 * 
	 * @param element
	 * @param ac
	 * @return
	 */
	public final String getETag(T element, InternalActionContext ac) {
		UserDao userDao = mesh().boot().userDao();
		RoleDao roleDao = mesh().boot().roleDao();

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
			HibRole role = roleDao.loadObjectByUuid(ac, roleUuid, READ_PERM);
			if (role != null) {
				Set<InternalPermission> permSet = roleDao.getPermissions(role, element);
				Set<String> humanNames = new HashSet<>();
				for (InternalPermission permission : permSet) {
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

	@Override
	public long globalCount() {
		return db().count(getPersistanceClass());
	}

	@Override
	public T create() {
		return getGraph().addFramedVertex(getPersistanceClass());
	}
}
