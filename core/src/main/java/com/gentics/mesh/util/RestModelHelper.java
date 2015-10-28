package com.gentics.mesh.util;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuidBlocking;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.handler.InternalActionContext;

public final class RestModelHelper {

	private RestModelHelper() {
	}

	public static void setRolePermissions(InternalActionContext ac, MeshVertex sourceElement, AbstractGenericRestResponse restModel) {
		String rolePermissionParameter = ac.getRolePermisssionParameter();
		if (!StringUtils.isEmpty(rolePermissionParameter)) {
			Role role = loadObjectByUuidBlocking(ac, rolePermissionParameter, READ_PERM, MeshRootImpl.getInstance().getRoleRoot());
			if (role != null) {
				Set<GraphPermission> permSet = role.getPermissions(sourceElement);
				Set<String> humanNames = new HashSet<>();
				for (GraphPermission permission : permSet) {
					humanNames.add(permission.getSimpleName());
				}
				String[] names = humanNames.toArray(new String[humanNames.size()]);
				restModel.setRolePerms(names);
			}
		}

	}

	public static void setRolePermissions(InternalActionContext ac, SchemaContainerImpl sourceElement, SchemaResponse restSchema) {
		String rolePermissionParameter = ac.getRolePermisssionParameter();

		if (!StringUtils.isEmpty(rolePermissionParameter)) {
			Role role = loadObjectByUuidBlocking(ac, rolePermissionParameter, READ_PERM, MeshRootImpl.getInstance().getRoleRoot());
			if (role != null) {
				Set<GraphPermission> permSet = role.getPermissions(sourceElement);
				Set<String> humanNames = new HashSet<>();
				for (GraphPermission permission : permSet) {
					humanNames.add(permission.getSimpleName());
				}
				String[] names = humanNames.toArray(new String[humanNames.size()]);
				restSchema.setRolePerms(names);
			}
		}

	}
}
