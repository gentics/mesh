package com.gentics.mesh.core.data.root;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.asset.AssetBinaryRoot;
import com.gentics.mesh.core.data.job.JobRoot;

/**
 * The mesh root is the primary graph element. All other aggregation nodes for users, roles, groups, projects connect to this element.
 */
public interface MeshRoot extends MeshVertex {

	public static final String MESH_VERSION = "meshVersion";

	/**
	 * Returns the mesh version which was last used to access the graph. This version is usually updated by the {@link BootstrapInitializer} during startup of
	 * mesh.
	 * 
	 * @return Plain text version or null if this information has not yet been stored
	 */
	String getMeshVersion();

	/**
	 * Set the plain text mesh version which was last used to access the graph.
	 * 
	 * @param version
	 */
	void setMeshVersion(String version);

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
	 * Returns the node aggregation vertex.
	 * 
	 * @return
	 */
	NodeRoot getNodeRoot();

	/**
	 * Returns the tag family aggregation vertex.
	 * 
	 * @return
	 */
	TagFamilyRoot getTagFamilyRoot();

	/**
	 * Return the asset binary aggregation vertex.
	 * 
	 * @return
	 */
	AssetBinaryRoot getAssetBinaryRoot();

	/**
	 * Returns the schema container aggregation vertex.
	 * 
	 * @return
	 */
	SchemaContainerRoot getSchemaContainerRoot();

	/**
	 * Returns the microschema container aggregation vertex.
	 * 
	 * @return
	 */
	MicroschemaContainerRoot getMicroschemaContainerRoot();

	/**
	 * This method will try to resolve the given path and return the element that is matching the path.
	 * 
	 * @param pathToElement
	 * @return Resolved element or null if no element could be found
	 */
	MeshVertex resolvePathToElement(String pathToElement);

}
