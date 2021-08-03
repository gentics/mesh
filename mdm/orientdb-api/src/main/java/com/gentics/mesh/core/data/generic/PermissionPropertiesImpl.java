package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.madl.traversal.TraversalResult;

import dagger.Lazy;

/**
 * @see PermissionProperties
 */
@Singleton
public class PermissionPropertiesImpl implements PermissionProperties {

	private final Lazy<RoleDaoWrapper> roleDaoLazy;

	@Inject
	public PermissionPropertiesImpl(Lazy<RoleDaoWrapper> roleDaoLazy) {
		this.roleDaoLazy = roleDaoLazy;
	}

	@Override
	public Result<? extends HibRole> getRolesWithPerm(HibBaseElement element, InternalPermission perm) {
		RoleDaoWrapper roleDao = roleDaoLazy.get();
		Set<String> roleUuids = roleDao.getRoleUuidsForPerm(element, perm);
		Stream<String> stream = roleUuids == null
			? Stream.empty()
			: roleUuids.stream();
		return new TraversalResult<>(stream
			.map(roleDao::findByUuidGlobal)
			.filter(Objects::nonNull));
	}

	@Override
	public PermissionInfo getRolePermissions(HibBaseElement element, InternalActionContext ac, String roleUuid) {
		if (!isEmpty(roleUuid)) {
			RoleDaoWrapper roleDao = roleDaoLazy.get();
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
