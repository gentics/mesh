package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class GroupTypeProvider {

	@Inject
	public GroupTypeProvider() {
	}

	public GraphQLObjectType getGroupType() {
		Builder groupType = newObject();
		groupType.name("group");
		groupType.description("Group description");
		groupType.field(newFieldDefinition().name("name").description("The name of the group").type(GraphQLString).build());
		groupType.field(newFieldDefinition().name("uuid").description("The uuid of the group").type(GraphQLString).build());
		return groupType.build();
	}
}
