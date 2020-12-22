package com.gentics.mesh.core.data.role;

import static com.gentics.mesh.core.rest.MeshEvent.ROLE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_UPDATED;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.role.RoleReference;

public interface HibRole extends HibCoreElement, HibUserTracking, HibNamedElement, HibBucketableElement {

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
}
