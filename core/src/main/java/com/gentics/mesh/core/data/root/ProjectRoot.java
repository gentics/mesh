package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;

import io.vertx.ext.web.RoutingContext;

/**
 * Aggregation node for projects.
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

	Project create(RoutingContext rc,  ProjectCreateRequest requestModel, MeshAuthUser requestUser);

}
