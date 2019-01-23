package com.gentics.mesh.core.data;

import static com.gentics.mesh.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.MeshEvent.PROJECT_UPDATED;

import java.util.Objects;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.madlmigration.TraversalResult;

/**
 * The Project Domain Model interface.
 *
 * Each mesh instance can store multiple projects. Each project is the root element for all project specific data. A project has a {@link Node} base element
 * (called basenode). Additionally languages and schemas can be assigned to projects to make them available for node creation. Various root vertices (eg.:
 * {@link NodeRoot}, {@link TagRoot}, {@link TagFamilyRoot} ) are linked to the project to store references to basic building blocks.
 */
public interface Project extends MeshCoreVertex<ProjectResponse, Project>, ReferenceableElement<ProjectReference>, UserTrackingVertex,
		IndexableElement {

	/**
	 * Type Value: {@value #TYPE}
	 */
	String TYPE = "project";

	TypeInfo TYPE_INFO = new TypeInfo(TYPE, PROJECT_CREATED.address, PROJECT_UPDATED.address, PROJECT_DELETED.address);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Compose the index name for the project index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return TYPE.toLowerCase();
	}

	/**
	 * Compose the document id for project index documents.
	 * 
	 * @param projectUuid
	 * @return
	 */
	static String composeDocumentId(String projectUuid) {
		Objects.requireNonNull(projectUuid, "A projectUuid must be provided.");
		return projectUuid;
	}

	/**
	 * Create the base node of the project using the user as a reference for the editor and creator fields.
	 * 
	 * @param creator
	 *            Creator of the base node
	 * @param schemaContainerVersion
	 *            Schema version used for the basenode creation
	 * 
	 * @return Created base node
	 */
	Node createBaseNode(User creator, SchemaContainerVersion schemaContainerVersion);

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
	 * Return the microschema container root for the project.
	 *
	 * @return
	 */
	MicroschemaContainerRoot getMicroschemaContainerRoot();

	/**
	 * Return a traversal result of languages that were assigned to the project.
	 * 
	 * @return
	 */
	TraversalResult<? extends Language> getLanguages();

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
	 * Return the node root aggregation vertex of the project. Internally this method will create the node root when it has not yet been created.
	 * 
	 * @return Node root element
	 */
	NodeRoot getNodeRoot();

	/**
	 * Get the initial branch of the project.
	 *
	 * @return
	 */
	Branch getInitialBranch();

	/**
	 * Get the latest branch of the project.
	 *
	 * @return
	 */
	Branch getLatestBranch();

	/**
	 * Return the branch root aggregation vertex of the project. Internally this method will create the branch root when it has not yet been created.
	 * 
	 * @return Branch root element
	 */
	BranchRoot getBranchRoot();

}
