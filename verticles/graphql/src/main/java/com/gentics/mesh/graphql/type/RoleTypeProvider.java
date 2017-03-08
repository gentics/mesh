package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Role;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class RoleTypeProvider extends AbstractTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public RoleTypeProvider() {
	}

	public Object groupFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof Role) {
			InternalActionContext ac = (InternalActionContext) env.getContext();
			return ((Role) source).getGroups(ac);
		}
		return null;
	}

	public GraphQLObjectType getRoleType() {
		Builder roleType = newObject();
		roleType.name("Role");
		roleType.description("Role description");
		interfaceTypeProvider.addCommonFields(roleType);

		// .name
		roleType.field(newFieldDefinition().name("name")
				.description("The name of the role")
				.type(GraphQLString)
				.build());

		// .groups
		roleType.field(newFieldDefinition().name("groups")
				.description("Groups which reference the role")
				.argument(getPagingArgs())
				.type(new GraphQLList(new GraphQLTypeReference("Group")))
				.dataFetcher(this::groupFetcher)
				.build());
		return roleType.build();
	}
}
