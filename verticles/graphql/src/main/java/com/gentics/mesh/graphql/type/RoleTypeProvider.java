package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.GroupTypeProvider.GROUP_PAGE_TYPE_NAME;
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

	public static final String ROLE_TYPE_NAME = "Role";
	
	public static final String ROLE_PAGE_TYPE_NAME = "RolesPage";

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public RoleTypeProvider() {
	}

	public GraphQLObjectType createType() {
		Builder roleType = newObject();
		roleType.name(ROLE_TYPE_NAME);
		roleType.description("Role description");
		interfaceTypeProvider.addCommonFields(roleType);

		// .name
		roleType.field(newFieldDefinition().name("name").description("The name of the role").type(GraphQLString).dataFetcher((env) -> {
			Role role = env.getSource();
			return role.getName();
		}));

		// .groups
		roleType.field(newPagingFieldWithFetcher("groups", "Groups which reference the role.", (env) -> {
			Role role = env.getSource();
			GraphQLContext gc = env.getContext();
			return role.getGroups(gc.getUser(), getPagingInfo(env));
		}, GROUP_PAGE_TYPE_NAME));
		return roleType.build();
	}

}