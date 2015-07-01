package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.MeshNode;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.rest.project.ProjectResponse;

public interface Project extends GenericNode {

	MeshNode getOrCreateRootNode();

	MeshNode getRootNode();

	TagFamilyRoot getTagFamilyRoot();

	SchemaContainerRoot getSchemaRoot();

	String getName();

	void setName(String name);

	void setRootNode(MeshNode rootNode);

	void delete();

	ProjectResponse transformToRest(MeshAuthUser requestUser);

	ProjectImpl getImpl();

	void setSchemaRoot(SchemaContainerRoot schemaRoot);

	TagFamilyRoot createTagFamilyRoot();

	void setTagFamilyRoot(TagFamilyRoot tagFamilyRoot);
}
