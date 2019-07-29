package com.gentics.mesh.plugin;

import graphql.schema.GraphQLObjectType;

public interface GraphQLPlugin extends MeshPlugin {

	/**
	 * Create the GraphQL API type for the plugin. Please note that this method will currently be invoked for each query. 
	 * It is thus recommended to generate the type up-front and only return the type field. 
	 * Doing otherwise could potentially slow down the GraphQL query.
	 *  
	 * @return
	 */
	GraphQLObjectType createType();

}
