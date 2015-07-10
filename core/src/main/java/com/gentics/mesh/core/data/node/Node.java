package com.gentics.mesh.core.data.node;

import java.io.IOException;
import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.FieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.util.InvalidArgumentException;

public interface Node extends ContainerNode {

	void addTag(Tag tag);

	void removeTag(Tag tag);

	SchemaContainer getSchemaContainer();

	Schema getSchema() throws IOException;

	void setSchemaContainer(SchemaContainer schema);

	void addProject(Project project);

	NodeFieldContainer getFieldContainer(Language language);

	List<? extends Tag> getTags();

	NodeFieldContainer getOrCreateFieldContainer(Language language);

	List<? extends FieldContainer> getFieldContainers();

	NodeResponse transformToRest(TransformationInfo info);

	String getNodeResponseJson(TransformationInfo info);

	NodeImpl getImpl();

	Page<? extends Node> getChildren(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	Page<? extends Tag> getTags(MeshAuthUser requestUser, String projectName, PagingInfo pagingInfo) throws InvalidArgumentException;

	void delete();

	void createLink(Node node);

}
