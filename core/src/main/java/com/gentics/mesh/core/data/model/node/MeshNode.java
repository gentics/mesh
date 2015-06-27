package com.gentics.mesh.core.data.model.node;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.FieldContainer;
import com.gentics.mesh.core.data.model.GenericNode;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.MeshAuthUser;
import com.gentics.mesh.core.data.model.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.model.MeshUser;
import com.gentics.mesh.core.data.model.Project;
import com.gentics.mesh.core.data.model.Schema;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.data.model.node.impl.MeshNodeImpl;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.node.response.NodeResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface MeshNode extends GenericNode {

	void addTag(Tag tag);

	void removeTag(Tag tag);

	Schema getSchema();

	void setSchema(Schema schema);

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

}
