package com.gentics.mesh.util;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

public final class RestModelHelper {

	private RestModelHelper() {
	}

	/**
	 * Examine the role permission parameter query value and set the role perms field in the rest model.
	 * 
	 * @param ac
	 * @param sourceElement
	 *            Element to be used for permission retrieval
	 * @param restModel
	 *            Rest model which contains the role permission field
	 */
	public static void setRolePermissions(InternalActionContext ac, GraphFieldSchemaContainer<?, ?, ?, ?> sourceElement,
			FieldSchemaContainer restModel) {
		String roleUuid = ac.getRolePermissionParameters().getRoleUuid();

		if (!isEmpty(roleUuid)) {
			Role role = MeshRootImpl.getInstance().getRoleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM).toBlocking().single();
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

}
