package com.gentics.mesh.core.data.model;

import com.gentics.mesh.core.data.model.impl.ProjectImpl;
import com.gentics.mesh.core.data.model.node.MeshNode;
import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.model.root.TagFamilyRoot;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;

public interface Project extends GenericNode {

	MeshNode getOrCreateRootNode();

	MeshNode getRootNode();

	TagFamilyRoot getTagFamilyRoot();

	SchemaRoot getSchemaRoot();

	String getName();

	void setName(String name);

	void setRootNode(MeshNode rootNode);

	void delete();

	ProjectResponse transformToRest(MeshAuthUser requestUser);

	ProjectImpl getImpl();

	void setSchemaRoot(SchemaRoot schemaRoot);

	TagFamilyRoot createTagFamilyRoot();

	void setTagFamilyRoot(TagFamilyRoot tagFamilyRoot);
}
