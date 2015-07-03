package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;

public interface TagFamily extends MeshVertex {

	String getName();

	void setName(String name);

	String getDescription();

	void setDescription(String description);

	Tag create(String name);

	void removeTag(Tag tag);

	void addTag(Tag tag);

	List<? extends Tag> getTags();

	TagFamilyImpl getImpl();

	TagFamilyResponse transformToRest(MeshAuthUser requestUser);
}
