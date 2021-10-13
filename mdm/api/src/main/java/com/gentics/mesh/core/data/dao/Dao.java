package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;

/**
 * General DAO.
 * 
 * @param <T>
 */
public interface Dao<T extends HibBaseElement> {

	/**
	 * Set the role permissionf for the given element.
	 * 
	 * @param element
	 * @param ac
	 * @param model
	 */
	void setRolePermissions(T element, InternalActionContext ac, GenericRestResponse model);

	/**
	 * Compare both values in order to determine whether the stored value should be updated.
	 * 
	 * @param restValue
	 *            Rest model value
	 * @param dbValue
	 *            Stored value
	 * @return true if restValue is not null and the restValue is not equal to the stored value. Otherwise false.
	 */
	default <E> boolean shouldUpdate(E restValue, E dbValue) {
		return restValue != null && !restValue.equals(dbValue);
	}

	/**
	 * Check if the context user has the requested permissions on the element.
	 * 
	 * @param element
	 * @param uuid
	 * @param ac
	 * @param perm
	 * @param errorIfNotFound
	 * @return
	 */
	default T checkPerms(T element, String uuid, InternalActionContext ac, InternalPermission perm, boolean errorIfNotFound) {
		if (element == null) {
			if (errorIfNotFound) {
				throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
			} else {
				return null;
			}
		}
		HibUser requestUser = ac.getUser();
		String elementUuid = element.getUuid();
		UserDao userDao = Tx.get().userDao();
		if (userDao.hasPermission(requestUser, element, perm)) {
			return element;
		} else {
			throw error(FORBIDDEN, "error_missing_perm", elementUuid, perm.getRestPerm().getName());
		}
	}

	/**
	 * Return the roles which grant the given permission on the element.
	 * 
	 * @param element
	 * @param perm
	 * @return
	 */
	default Result<? extends HibRole> getRolesWithPerm(T element, InternalPermission perm) {
		RoleDao roleDao = Tx.get().roleDao();
		Set<String> roleUuids = roleDao.getRoleUuidsForPerm(element, perm);
		Stream<String> stream = roleUuids == null
			? Stream.empty()
			: roleUuids.stream();
		return new TraversalResult<>(stream
			.map(roleDao::findByUuid)
			.filter(Objects::nonNull));
	}

	/**
	 * Return the permission info for the given element and role.
	 * 
	 * @param element
	 * @param ac
	 * @param roleUuid
	 * @return
	 */
	default PermissionInfo getRolePermissions(HibCoreElement<? extends RestModel> element, InternalActionContext ac, String roleUuid) {
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
