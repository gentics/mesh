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
public class GroupTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public ArgumentsProvider argumentsProvider;

	@Inject
	public UserTypeProvider userTypeProvider;


	@Inject
	public GroupTypeProvider() {
	}

	public GraphQLObjectType getGroupType() {
		Builder groupType = newObject();
		groupType.name("Group");
		groupType.description("Group description");
		interfaceTypeProvider.addCommonFields(groupType);
		groupType.field(newFieldDefinition().name("name").description("The name of the group").type(GraphQLString).build());

		groupType.field(newFieldDefinition().name("roles").description("Roles assigned to the group").argument(argumentsProvider.getPagingArgs())
				.type(new GraphQLList(new GraphQLTypeReference("Role"))).build());
		groupType.field(newFieldDefinition().name("users").description("Users assigned to the group").argument(argumentsProvider.getPagingArgs())
				.type(new GraphQLList(new GraphQLTypeReference("User"))).build());
		return groupType.build();
	}
}
