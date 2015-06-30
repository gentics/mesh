package com.gentics.mesh.core.data.node;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.MeshUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.impl.MeshNodeImpl;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.node.response.NodeResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface MeshNode extends GenericNode {

	void addTag(Tag tag);

	void removeTag(Tag tag);

	SchemaContainer getSchema();

	void setSchemaContainer(SchemaContainer schema);

	void setCreator(MeshUser user);

	void addProject(Project project);

	void setParentNode(MeshNode parentNode);

	MeshNode create();

	MeshNodeFieldContainer getFieldContainer(Language language);

	List<? extends MeshNode> getChildren();

	List<? extends Tag> getTags();

	MeshNodeFieldContainer getOrCreateFieldContainer(Language language);

	List<? extends FieldContainer> getFieldContainers();

	NodeResponse transformToRest(TransformationInfo info);

	MeshUser getCreator();

	MeshNodeImpl getImpl();

	Page<MeshNode> getChildren(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	Page<Tag> getTags(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	void delete();

}
