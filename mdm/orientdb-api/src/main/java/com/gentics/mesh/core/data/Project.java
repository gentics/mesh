package com.gentics.mesh.core.data;

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
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
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
public interface Project extends MeshCoreVertex<ProjectResponse>, ReferenceableElement<ProjectReference>, UserTrackingVertex, HibUserTracking, HibProject, GraphDBBucketableElement {

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
	 * Return the node root aggregation vertex of the project. Internally this method will create the node root when it has not yet been created.
	 * 
	 * @return Node root element
	 */
	NodeRoot getNodeRoot();

	/**
	 * Return the branch root aggregation vertex of the project. Internally this method will create the branch root when it has not yet been created.
	 * 
	 * @return Branch root element
	 */
	BranchRoot getBranchRoot();

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

	@Override
	default Result<? extends HibSchema> getSchemas() {
		return getSchemaContainerRoot().findAll();
	}

	@Override
	default Result<? extends HibMicroschema> getMicroschemas() {
		return getMicroschemaContainerRoot().findAll();
	}
}
