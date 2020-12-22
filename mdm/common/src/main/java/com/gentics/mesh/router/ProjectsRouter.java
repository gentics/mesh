package com.gentics.mesh.router;

import java.util.Map;

import javax.naming.InvalidNameException;

import io.vertx.ext.web.Router;

public interface ProjectsRouter {

	/**
	 * Common router which holds project specific routes (e.g: /nodes /tagFamilies)
	 * 
	 * @return
	 */
	ProjectRouter projectRouter();

	/**
	 * Fail if the provided name is invalid or would cause a conflicts with an existing API router.
	 * 
	 * @param name
	 *            Project name to be checked
	 */
	void assertProjectNameValid(String name);

	/**
	 * Add a new project router with the given name to the projects router. This method will return an existing router when one already has been setup.
	 * 
	 * @param name
	 *            Name of the project router
	 * @return Router for the given project name
	 * @throws InvalidNameException
	 */
	Router addProjectRouter(String name) throws InvalidNameException;

	/**
	 * Check whether the project router for the given project name is already registered.
	 * 
	 * @param projectName
	 * @return
	 */
	boolean hasProjectRouter(String projectName);

	Map<String, Router> getProjectRouters();

}
