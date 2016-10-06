package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class ProjectTypeProvider {

	private NodeTypeProvider nodeTypeProvider;

	@Inject
	public ProjectTypeProvider(NodeTypeProvider nodeTypeProvider) {
		this.nodeTypeProvider = nodeTypeProvider;
	}

	public GraphQLObjectType getProjectType() {
		Builder root = newObject();
		root.name("Project");
		root.field(newFieldDefinition().name("name").description("The name of the project").type(GraphQLString).build());
		root.field(newFieldDefinition().name("uuid").description("The uuid of the project").type(GraphQLString).build());
		root.field(newFieldDefinition().name("baseNode").description("The base node of the project").type(nodeTypeProvider.getNodeType()).build());
		return root.build();
	}

}
