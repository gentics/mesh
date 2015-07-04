package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.rest.project.ProjectResponse;

public interface Project extends GenericNode {

	Node getOrCreateRootNode();

	Node getRootNode();

	TagFamilyRoot getTagFamilyRoot();

	SchemaContainerRoot getSchemaRoot();

	String getName();

	void setName(String name);

	void setRootNode(Node rootNode);

	void delete();

	ProjectResponse transformToRest(MeshAuthUser requestUser);

	ProjectImpl getImpl();

	void setSchemaRoot(SchemaContainerRoot schemaRoot);

	TagFamilyRoot createTagFamilyRoot();

	void setTagFamilyRoot(TagFamilyRoot tagFamilyRoot);

	List<? extends Language> getLanguages();

	void removeLanguage(Language language);

	void addLanguage(Language language);
}
