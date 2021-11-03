package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.dao.Dao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;

/**
 * @deprecated The functionality moved into {@link Dao} - remove this class hierarchy.
 * @author plyhun
 *
 */
public interface PermissionProperties {

	default Result<? extends HibRole> getRolesWithPerm(HibBaseElement element, InternalPermission perm) {
		RoleDao roleDao = Tx.get().roleDao();
		Set<String> roleUuids = roleDao.getRoleUuidsForPerm(element, perm);
		Stream<String> stream = roleUuids == null
			? Stream.empty()
			: roleUuids.stream();
		return new TraversalResult<>(stream
			.map(roleDao::findByUuid)
			.filter(Objects::nonNull));
	}

	default PermissionInfo getRolePermissions(HibBaseElement element, InternalActionContext ac, String roleUuid) {
		if (!isEmpty(roleUuid)) {
			RoleDao roleDao = Tx.get().roleDao();
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
