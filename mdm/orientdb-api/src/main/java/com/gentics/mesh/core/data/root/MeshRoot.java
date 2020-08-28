package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.HibMeshVersion;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.changelog.ChangelogRoot;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.node.Node;

/**
 * The mesh root is the primary graph element. All other aggregation nodes for users, roles, groups, projects connect to this element.
 */
public interface MeshRoot extends MeshVertex, HibMeshVersion {

	public static final String MESH_VERSION = "meshVersion";

	public static final String MESH_DB_REV = "meshDatabaseRevision";

	/**
	 * Returns the user aggregation vertex.
	 * 
	 * @return
	 */
	UserRoot getUserRoot();

	/**
	 * Returns the group aggregation vertex.
	 * 
	 * @return
	 */
	GroupRoot getGroupRoot();

	/**
	 * Returns the role aggregation vertex.
	 * 
	 * @return
	 */
	RoleRoot getRoleRoot();

	/**
	 * Returns the job queue vertex.
	 * 
	 * @return
	 */
	JobRoot getJobRoot();

	/**
	 * Returns the language aggregation vertex.
	 * 
	 * @return
	 */
	LanguageRoot getLanguageRoot();

	/**
	 * Returns the project aggregation vertex.
	 * 
	 * @return
	 */
	ProjectRoot getProjectRoot();

	/**
	 * Returns the tag aggregation vertex.
	 * 
	 * @return
	 */
	TagRoot getTagRoot();

	/**
	 * Returns the tag family aggregation vertex.
	 * 
	 * @return
	 */
	TagFamilyRoot getTagFamilyRoot();

	/**
	 * Return the changelog aggregation vertex.
	 * @return
	 */
	ChangelogRoot getChangelogRoot();

	/**
	 * Returns the schema container aggregation vertex.
	 * 
	 * @return
	 */
	SchemaRoot getSchemaContainerRoot();

	/**
	 * Returns the microschema container aggregation vertex.
	 * 
	 * @return
	 */
	MicroschemaRoot getMicroschemaContainerRoot();

	/**
	 * This method will try to resolve the given path and return the element that is matching the path.
	 * 
	 * @param pathToElement
	 * @return Resolved element or null if no element could be found
	 */
	HibElement resolvePathToElement(String pathToElement);

	void clearReferences();

	Node findNodeByUuid(String uuid);

}
