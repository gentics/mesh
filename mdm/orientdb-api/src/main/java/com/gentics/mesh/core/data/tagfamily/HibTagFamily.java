package com.gentics.mesh.core.data.tagfamily;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;

public interface HibTagFamily extends HibCoreElement, HibUserTracking {

	/**
	 * Convert this back to the non-mdm tagfamily
	 * 
	 * @return
	 * @deprecated This method should only be used when there is really no other way
	 */
	@Deprecated
	default TagFamily toTagFamily() {
		return (TagFamily) this;
	}

	void setName(String name);

	String getName();

	HibProject getProject();

	void deleteElement();

	String getElementVersion();

	TagFamilyReference transformToReference();

	String getDescription();

	void setDescription(String description);

}
