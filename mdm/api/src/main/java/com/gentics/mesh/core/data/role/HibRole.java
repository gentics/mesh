package com.gentics.mesh.core.data.role;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.role.RoleReference;

public interface HibRole extends HibCoreElement, HibUserTracking, HibNamedElement {

	void removeElement();

	RoleReference transformToReference();

	String getElementVersion();

}
