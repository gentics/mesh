package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.madl.traversal.TraversalResult;

@Singleton
public class PermissionProperties {

	private final BootstrapInitializer boot;

	@Inject
	public PermissionProperties(BootstrapInitializer boot) {
		this.boot = boot;
	}

	public TraversalResult<? extends HibRole> getRolesWithPerm(HibElement element, InternalPermission perm) {
		Set<String> roleUuids = element.getRoleUuidsForPerm(perm);
		Stream<String> stream = roleUuids == null
			? Stream.empty()
			: roleUuids.stream();
		RoleDaoWrapper roleDao = boot.roleDao();
		return new TraversalResult<>(stream
			.map(roleDao::findByUuid)
			.filter(Objects::nonNull));
	}

	public PermissionInfo getRolePermissions(HibElement element, InternalActionContext ac, String roleUuid) {
		if (!isEmpty(roleUuid)) {
			RoleDaoWrapper roleDao = boot.roleDao();
			HibRole role = roleDao.loadObjectByUuid(ac, roleUuid, READ_PERM);
			if (role != null) {
				PermissionInfo permissionInfo = new PermissionInfo();
				Set<InternalPermission> permSet = roleDao.getPermissions(role, element);
				for (InternalPermission permission : permSet) {
					permissionInfo.set(permission.getRestPerm(), true);
				}
				permissionInfo.setOthers(false);
				return permissionInfo;
			}
		}
		return null;
	}
}
