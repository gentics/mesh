package com.gentics.mesh.core.data.role;

import static com.gentics.mesh.core.rest.MeshEvent.ROLE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_UPDATED;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;

/**
 * Domain model for role.
 */
public interface HibRole extends HibNamedElement<RoleResponse>, HibUserTracking, HibBucketableElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.ROLE, ROLE_CREATED, ROLE_UPDATED, ROLE_DELETED);

	/**
	 * Remove the element.
	 */
	void removeElement();

	/**
	 * Transform the role to a reference POJO.
	 * 
	 * @return
	 */
	RoleReference transformToReference();

	/**
	 * Return the current element version.
	 * 
	 * TODO: Check how versions can be accessed via Hibernate and refactor / remove this method accordingly
	 * 
	 * @return
	 */
	String getElementVersion();

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return a result of groups to which this role was assigned.
	 * 
	 * @return Result
	 */
	Result<? extends HibGroup> getGroups();
}
