package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class UserTypeProvider {

	@Inject
	public UserTypeProvider() {
	}

	public GraphQLObjectType getUserType() {
		Builder root = newObject();
		root.name("user");
		root.description("User description");
		root.field(newFieldDefinition().name("username").description("The username of the user ").type(GraphQLString).build());
		root.field(newFieldDefinition().name("firstname").description("The firstname of the user ").type(GraphQLString).build());
		root.field(newFieldDefinition().name("lastname").description("The lastname of the user ").type(GraphQLString).build());
		root.field(newFieldDefinition().name("emailAddress").description("The email of the user ").type(GraphQLString).build());
		root.field(newFieldDefinition().name("uuid").description("The uuid of the user ").type(GraphQLString).build());
		return root.build();
	}
}
