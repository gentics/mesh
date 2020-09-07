package com.gentics.mesh.core.data.group;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.group.GroupReference;

public interface HibGroup extends HibCoreElement, HibUserTracking, HibNamedElement {

	GroupReference transformToReference();

	void removeElement();

	String getElementVersion();

}
