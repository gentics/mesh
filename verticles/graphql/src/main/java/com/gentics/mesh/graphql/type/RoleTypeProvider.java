package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class RoleTypeProvider extends AbstractTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public RoleTypeProvider() {
	}

	public GraphQLObjectType createRoleType() {
		Builder roleType = newObject();
		roleType.name("Role");
		roleType.description("Role description");
		interfaceTypeProvider.addCommonFields(roleType);

		// .name
		roleType.field(newFieldDefinition().name("name")
				.description("The name of the role")
				.type(GraphQLString)
				.dataFetcher((env) -> {
					Role role = env.getSource();
					return role.getName();
				}));

		// .groups
		roleType.field(newPagingFieldWithFetcher("groups", "Groups which reference the role.", (env) -> {
			Role role = env.getSource();
			GraphQLContext gc = env.getContext();
			return role.getGroups(gc.getUser(), createPagingParameters(env));
		}, "Group"));
		return roleType.build();
	}
}
