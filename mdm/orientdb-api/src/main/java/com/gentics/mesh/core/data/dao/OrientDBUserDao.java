package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.HibUser;

public interface OrientDBUserDao extends UserDao {

	/**
	 * Check whether the user has the given permission on the given element.
	 *
	 * @param user
	 * @param element
	 * @param permission
	 * @return
	 * @deprecated Use {@link #hasPermission(HibUser, HibBaseElement, InternalPermission)} instead.
	 */
	@Deprecated
	boolean hasPermission(HibUser user, MeshVertex element, InternalPermission permission);

}
