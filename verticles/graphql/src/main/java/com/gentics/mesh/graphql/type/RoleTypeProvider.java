package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class RoleTypeProvider {

	@Inject
	public RoleTypeProvider() {
	}

	public GraphQLObjectType getRoleType() {
		Builder roleType = newObject();
		roleType.name("role");
		roleType.description("Role description");
		roleType.field(newFieldDefinition().name("name").description("The name of the role").type(GraphQLString).build());
		roleType.field(newFieldDefinition().name("uuid").description("The uuid of the role").type(GraphQLString).build());
		return roleType.build();
	}
}
