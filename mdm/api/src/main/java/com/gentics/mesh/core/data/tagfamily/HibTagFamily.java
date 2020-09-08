package com.gentics.mesh.core.data.tagfamily;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;

public interface HibTagFamily extends HibCoreElement, HibUserTracking {

	void setName(String name);

	String getName();

	HibProject getProject();

	void deleteElement();

	String getElementVersion();

	TagFamilyReference transformToReference();

	String getDescription();

	void setDescription(String description);

}
