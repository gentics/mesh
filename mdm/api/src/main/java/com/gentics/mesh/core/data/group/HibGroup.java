package com.gentics.mesh.core.data.group;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.group.GroupReference;

public interface HibGroup extends HibCoreElement, HibUserTracking, HibNamedElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.GROUP, GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	GroupReference transformToReference();

	void removeElement();

	String getElementVersion();

}
