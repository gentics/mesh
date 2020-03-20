package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.madl.traversal.TraversalResult;

@Singleton
public class PermissionPropertiesImpl implements PermissionProperties {

	private final BootstrapInitializer boot;

	@Inject
	public PermissionPropertiesImpl(BootstrapInitializer boot) {
		this.boot = boot;
	}

	public TraversalResult<? extends Role> getRolesWithPerm(MeshVertex vertex, GraphPermission perm) {
		Set<String> roleUuids = vertex.property(perm.propertyKey());
		Stream<String> stream = roleUuids == null
			? Stream.empty()
			: roleUuids.stream();
		RoleRoot roleRoot = boot.roleRoot();
		return new TraversalResult<>(stream
			.map(roleRoot::findByUuid)
			.filter(Objects::nonNull)
		);
	}

	public PermissionInfo getRolePermissions(MeshVertex vertex, InternalActionContext ac, String roleUuid) {
		if (!isEmpty(roleUuid)) {
			Role role = boot.roleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);
			if (role != null) {
				PermissionInfo permissionInfo = new PermissionInfo();
				Set<GraphPermission> permSet = role.getPermissions(vertex);
				for (GraphPermission permission : permSet) {
					permissionInfo.set(permission.getRestPerm(), true);
				}
				permissionInfo.setOthers(false);
				return permissionInfo;
			}
		}
		return null;
	}
}
