package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;

import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.MicroschemaRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.rest.event.project.ProjectMicroschemaEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;

/**
 * The Project Domain Model interface.
 *
 * Each mesh instance can store multiple projects. Each project is the root element for all project specific data. A project has a {@link Node} base element
 * (called basenode). Additionally languages and schemas can be assigned to projects to make them available for node creation. Various root vertices (eg.:
 * {@link NodeRoot}, {@link TagRoot}, {@link TagFamilyRoot} ) are linked to the project to store references to basic building blocks.
 */
public interface Project extends MeshCoreVertex<ProjectResponse, Project>, ReferenceableElement<ProjectReference>, UserTrackingVertex, HibUserTracking, HibProject {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.PROJECT, PROJECT_CREATED, PROJECT_UPDATED, PROJECT_DELETED);

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
		return "project";
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
	 * @param schemaVersion
	 *            Schema version used for the basenode creation
	 * 
	 * @return Created base node
	 */
	Node createBaseNode(HibUser creator, SchemaVersion schemaVersion);

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
	SchemaRoot getSchemaContainerRoot();

	/**
	 * Return the microschema container root for the project.
	 *
	 * @return
	 */
	MicroschemaRoot getMicroschemaContainerRoot();

	/**
	 * Return a traversal result of languages that were assigned to the project.
	 * 
	 * @return
	 */
	Result<? extends Language> getLanguages();

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
	HibBranch getInitialBranch();

	/**
	 * Get the latest branch of the project.
	 *
	 * @return
	 */
	HibBranch getLatestBranch();

	/**
	 * Return the branch root aggregation vertex of the project. Internally this method will create the branch root when it has not yet been created.
	 * 
	 * @return Branch root element
	 */
	BranchRoot getBranchRoot();

	/**
	 * Create a project schema assignment event.
	 * 
	 * @param schema
	 * @param assigned
	 * @return
	 */
	ProjectSchemaEventModel onSchemaAssignEvent(HibSchema schema, Assignment assigned);

	/**
	 * Create a project microschema assignment event.
	 * 
	 * @param microschema
	 * @param assigned
	 * @return
	 */
	ProjectMicroschemaEventModel onMicroschemaAssignEvent(HibMicroschema microschema, Assignment assigned);

	/**
	 * Find the branch via name or uuid that belongs to the project.
	 * 
	 * @param branchNameOrUuid
	 * @return
	 */
	HibBranch findBranch(String branchNameOrUuid);

	/**
	 * Find the branch via name or uuid that belongs to the project.
	 * Returns the latest branch if the branch could not be found.
	 *
	 * @param branchNameOrUuid
	 * @return
	 */
	HibBranch findBranchOrLatest(String branchNameOrUuid);

	/**
	 * Find all nodes that belong to the project.
	 * 
	 * @return
	 */
	Result<? extends Node> findNodes();

	/**
	 * Find a node in this project. Null if the node could not be found.
	 * @param uuid
	 * @return
	 */
	Node findNode(String uuid);
}
