package com.gentics.mesh.plugin;

import graphql.schema.GraphQLObjectType;

public interface GraphQLPlugin extends MeshPlugin {

	GraphQLObjectType createType();

}
