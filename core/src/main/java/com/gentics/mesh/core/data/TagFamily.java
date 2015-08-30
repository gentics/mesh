package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.util.InvalidArgumentException;

public interface TagFamily extends GenericVertex<TagFamilyResponse>, NamedVertex, IndexedVertex {

	public static final String TYPE = "tagFamily";

	String getDescription();

	void setDescription(String description);

	Tag create(String name, Project project, User creator);

	void removeTag(Tag tag);

	void addTag(Tag tag);

	List<? extends Tag> getTags();

	Page<? extends Tag> getTags(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;

	TagFamilyImpl getImpl();

	Tag findTagByName(String name);
}
