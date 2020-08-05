package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Project Root Node domain model interface.
 * 
 * <pre>
* {@code
* 	(pr:ProjectRootImpl)-[r1:HAS_PROJECT]->(p1:ProjectImpl)
* 	(pr-[r2:HAS_PROJECT]->(p2:ProjectImpl)
 	(pr)-[r3:HAS_PROJECT]->(p3:ProjectImpl)
 * 	(mr:MeshRootImpl)-[r:HAS_PROJECT_ROOT]->(pr)
* }
 * </pre>
 *
 * <p>
 * <img src="http://getmesh.io/docs/javadoc/cypher/com.gentics.mesh.core.data.root.impl.ProjectRootImpl.jpg" alt="">
 * </p>
 */
public interface ProjectRoot extends RootVertex<Project> {

	public static final String TYPE = "projects";

	/**
	 * Create a new project with the given name and add it to the aggregation vertex. Assign the provided schema container version to the created initial branch
	 * of the project.
	 * 
	 * @param projectName
	 *            Name of the new project.
	 * @param hostname
	 *            Hostname of the project which will be assigend to the initial branch.
	 * @param ssl
	 *            SSL flag of the project which will be assigned to the initial branch.
	 * @param pathPrefix
	 *            Path prefix which will be assigned to the initial branch.
	 * @param creator
	 *            User that is being used to set the initial creator and editor references.
	 * @param schemaContainerVersion
	 *            Schema container version which will be used to create the basenode
	 * @param batch
	 * @return
	 */
	default Project create(String projectName, String hostname, Boolean ssl, String pathPrefix, User creator,
		SchemaContainerVersion schemaContainerVersion, EventQueueBatch batch) {
		return create(projectName, hostname, ssl, pathPrefix, creator, schemaContainerVersion, null, batch);
	}

	/**
	 * Create a new project with the given name and add it to the aggregation vertex. Assign the provided schema container version to the created initial branch
	 * of the project.
	 * 
	 * @param projectName
	 *            Name of the new project.
	 * @param hostname
	 *            Hostname of the project which will be assigned to the initial branch.
	 * @param ssl
	 *            SSL flag of the project which will be assigned to the initial branch.
	 * @param pathPrefix
	 *            Path prefix which will be assigned to the initial branch.
	 * @param creator
	 *            User that is being used to set the initial creator and editor references.
	 * @param schemaContainerVersion
	 *            Schema container version which will be used to create the basenode
	 * @param uuid
	 *            Optional uuid
	 * @param batch
	 * @return
	 */
	Project create(String projectName, String hostname, Boolean ssl, String pathPrefix, User creator, SchemaContainerVersion schemaContainerVersion,
		String uuid, EventQueueBatch batch);

	/**
	 * Remove the project from the aggregation vertex.
	 * 
	 * @param project
	 */
	void removeProject(Project project);

	/**
	 * Add given the project to the aggregation vertex.
	 * 
	 * @param project
	 */
	void addProject(Project project);

}
