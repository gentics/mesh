package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;

/**
 * General DAO.
 * 
 * @param <T>
 */
public interface Dao<T extends HibBaseElement> {

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
}
