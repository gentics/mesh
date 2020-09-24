package com.gentics.mesh.core.data.tag;

import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.tag.TagReference;

public interface HibTag extends HibCoreElement, HibUserTracking, HibBucketableElement {

	String getName();

	HibTagFamily getTagFamily();

	void setName(String name);

	HibProject getProject();

	void deleteElement();

	TagReference transformToReference();

	String getElementVersion();
}
