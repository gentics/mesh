package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Role;

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
				.type(GraphQLString));

		// .groups
		roleType.field(newPagingFieldWithFetcher("groups", "Groups which reference the role.", (env) -> {
			Role role = env.getSource();
			InternalActionContext ac = env.getContext();
			return role.getGroups(ac.getUser(), getPagingParameters(env));
		}, "Group"));
		return roleType.build();
	}
}
