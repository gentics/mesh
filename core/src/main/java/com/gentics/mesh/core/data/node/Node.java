package com.gentics.mesh.core.data.node;

import java.io.IOException;
import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.paging.PagingInfo;

public interface Node extends GenericNode {

	void addTag(Tag tag);

	void removeTag(Tag tag);

	SchemaContainer getSchemaContainer();

	Schema getSchema() throws IOException;

	void setSchemaContainer(SchemaContainer schema);

	void setCreator(User user);

	void addProject(Project project);

	void setParentNode(Node parentNode);

	Node create();

	NodeFieldContainer getFieldContainer(Language language);

	List<? extends Node> getChildren();

	List<? extends Tag> getTags();

	NodeFieldContainer getOrCreateFieldContainer(Language language);

	List<? extends FieldContainer> getFieldContainers();

	NodeResponse transformToRest(TransformationInfo info);

	User getCreator();

	NodeImpl getImpl();

	Page<Node> getChildren(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	Page<Tag> getTags(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	void delete();

}
