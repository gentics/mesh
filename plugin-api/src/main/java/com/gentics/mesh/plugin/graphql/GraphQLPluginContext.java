package com.gentics.mesh.plugin.graphql;

import io.vertx.core.json.JsonObject;

/**
 * The public API that a plugin can use to access context information.
 */
public interface GraphQLPluginContext {

	/**
	 * Return the project name that is currently used.
	 * 
	 * @return
	 */
	String projectName();

	/**
	 * Return the project uuid that is currently used.
	 * 
	 * @return
	 */
	String projectUuid();

	/**
	 * return the branch name for which the request was executed.
	 * 
	 * @return
	 */
	String branchName();

	/**
	 * Return the branch uuid for which the request was executed.
	 * 
	 * @return
	 */
	String branchUuid();

	/**
	 * Returns the principal of the user that is linked to the currently executed request.
	 * 
	 * @return
	 */
	JsonObject principal();
}
