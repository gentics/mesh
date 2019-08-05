package com.gentics.mesh.plugin.graphql;

import com.gentics.mesh.plugin.MeshPlugin;

import graphql.schema.GraphQLSchema;

/**
 * A GraphQL Plugin is an plugin which will extend the GraphQL API of Gentics Mesh.
 */
public interface GraphQLPlugin extends MeshPlugin {

	/**
	 * Create the GraphQL API root schema for the plugin. Please note that this method will be invoked for each query. It is thus recommended to generate the
	 * type up-front and only return the type field. Doing otherwise could potentially slow down the GraphQL query.
	 * 
	 * @return
	 */
	GraphQLSchema createRootSchema();

	/**
	 * Prefix the type of the plugin with the plugin id to avoid conflicts with Gentics Mesh or other plugin types.
	 * 
	 * @param type
	 * @return Prefixed type
	 */
	default String prefixType(String type) {
		// TODO sanitize id
		return id() + "_" + type;
	}

	/**
	 * Return the name for the graphql API field. By default the plugin id will be used for the name.
	 */
	default String gqlApiName() {
		return id();
	}
}
