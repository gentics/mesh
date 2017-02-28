package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.graphql.type.argument.ArgumentsProvider;

import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class UserTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public ArgumentsProvider argumentsProvider;

	@Inject
	public UserTypeProvider() {
	}

	public GraphQLObjectType getUserType() {
		Builder root = newObject();
		root.name("User");
		root.description("User description");
		interfaceTypeProvider.addCommonFields(root);
		root.field(newFieldDefinition().name("username").description("The username of the user").type(GraphQLString).build());
		root.field(newFieldDefinition().name("firstname").description("The firstname of the user").type(GraphQLString).build());
		root.field(newFieldDefinition().name("lastname").description("The lastname of the user").type(GraphQLString).build());
		root.field(newFieldDefinition().name("emailAddress").description("The email of the user").type(GraphQLString).build());
		root.field(newFieldDefinition().name("groups").description("Groups to which the user belongs").argument(argumentsProvider.getPagingArgs())
				.type(new GraphQLList(new GraphQLTypeReference("Group"))).build());
		//TODO handle project
		root.field(newFieldDefinition().name("nodeReference").description("User node reference").type(new GraphQLTypeReference("Node")).build());
		return root.build();
	}
}
