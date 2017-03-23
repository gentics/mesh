package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class GroupTypeProvider extends AbstractTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public UserTypeProvider userTypeProvider;

	@Inject
	public GroupTypeProvider() {
	}

	public GraphQLObjectType createGroupType() {
		Builder groupType = newObject();
		groupType.name("Group");
		groupType.description("A group is a collection of users. Groups can't be nested.");
		interfaceTypeProvider.addCommonFields(groupType);

		// .name
		groupType.field(newFieldDefinition().name("name")
				.description("The name of the group.")
				.type(GraphQLString));

		// .roles
		groupType.field(newPagingFieldWithFetcher("roles", "Roles assigned to the group.", (env) -> {
			GraphQLContext gc = env.getContext();
			Group group = env.getSource();
			return group.getRoles(gc.getUser(), getPagingInfo(env));
		}, "Role"));

		// .users
		groupType.field(newPagingFieldWithFetcher("users", "Users assigned to the group.", (env) -> {
			GraphQLContext gc = env.getContext();
			Group group = env.getSource();
			return group.getVisibleUsers(gc.getUser(), getPagingInfo(env));
		}, "User"));
		return groupType.build();
	}
}
