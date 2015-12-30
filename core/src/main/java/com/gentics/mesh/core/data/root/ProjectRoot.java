package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;

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
	 * Create a new project with the given name and add it to the aggregation vertex.
	 * 
	 * @param projectName
	 *            Name of the new project.
	 * @param creator
	 *            User that is being used to set the initial creator and editor references.
	 * @return
	 */
	Project create(String projectName, User creator);

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
