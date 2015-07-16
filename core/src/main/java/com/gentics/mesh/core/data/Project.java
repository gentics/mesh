package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.rest.project.ProjectResponse;

public interface Project extends GenericVertex<ProjectResponse>, NamedNode {

	Node createBaseNode(User creator);

	Node getBaseNode();

	void setBaseNode(Node baseNode);

	TagFamilyRoot getTagFamilyRoot();

	SchemaContainerRoot getSchemaRoot();

	void setSchemaRoot(SchemaContainerRoot schemaRoot);

	TagFamilyRoot createTagFamilyRoot();

	void setTagFamilyRoot(TagFamilyRoot tagFamilyRoot);

	List<? extends Language> getLanguages();

	void removeLanguage(Language language);

	void addLanguage(Language language);

	TagRoot getTagRoot();

	TagRoot createTagRoot();

	NodeRoot getNodeRoot();

	NodeRoot createNodeRoot();

	void setTagRoot(TagRoot root);

	void setNodeRoot(NodeRoot root);

	ProjectImpl getImpl();
}
