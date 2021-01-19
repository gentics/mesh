package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.GroupTypeProvider.GROUP_PAGE_TYPE_NAME;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

/**
 * GraphQL type provider for role.
 */
@Singleton
public class RoleTypeProvider extends AbstractTypeProvider {

	public static final String ROLE_TYPE_NAME = "Role";

	public static final String ROLE_PAGE_TYPE_NAME = "RolesPage";

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public RoleTypeProvider(MeshOptions options) {
		super(options);
	}

	/**
	 * Create the role type.
	 * 
	 * @return
	 */
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
		roleType.field(newPagingFieldWithFetcher("groups", "Groups which reference the role.", env -> {

			RoleDaoWrapper roleDao = Tx.get().roleDao();
			HibRole role = env.getSource();
			GraphQLContext gc = env.getContext();

			return roleDao.getGroups(role, gc.getUser(), getPagingInfo(env));
		}, GROUP_PAGE_TYPE_NAME));
		return roleType.build();
	}

}