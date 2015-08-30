package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.rest.project.ProjectResponse;

public interface Project extends GenericVertex<ProjectResponse>, NamedVertex, IndexedVertex {

	public static final String TYPE = "project";

	Node createBaseNode(User creator);

	/**
	 * Return the base node of the project.
	 * 
	 * @return
	 */
	Node getBaseNode();

	/**
	 * Set the base node for the project.
	 * 
	 * @param baseNode
	 */
	void setBaseNode(Node baseNode);

	/**
	 * Return the tagFamilyRoot for the project. This method will create a new tag family root when no one could be found.
	 * 
	 * @return
	 */
	TagFamilyRoot getTagFamilyRoot();

	/**
	 * Return the schema container root for the project.
	 * 
	 * @return
	 */
	SchemaContainerRoot getSchemaContainerRoot();

	/**
	 * Return a list of languages that were assigned to the project.
	 * 
	 * @return
	 */
	List<? extends Language> getLanguages();

	/**
	 * Unassign the language from the project.
	 * 
	 * @param language
	 */
	void removeLanguage(Language language);

	/**
	 * Assign the given language to the project.
	 * 
	 * @param language
	 */
	void addLanguage(Language language);

	/**
	 * Return the tag root aggregation vertex of the project. Internally this method will create the tag root when it has not yet been created.
	 * 
	 * @return
	 */
	TagRoot getTagRoot();

	/**
	 * Return the node root aggregation vertex of the project. Internally this method will create the node root when it has not yet been created.
	 * 
	 * @return
	 */
	NodeRoot getNodeRoot();

}
