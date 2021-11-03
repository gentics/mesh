package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.graphql.type.GroupTypeProvider.GROUP_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.RoleTypeProvider.ROLE_PAGE_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class UserTypeProvider extends AbstractTypeProvider {

	public static final String USER_TYPE_NAME = "User";

	public static final String USER_PAGE_TYPE_NAME = "UsersPage";

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public UserTypeProvider(MeshOptions options) {
		super(options);
	}

	public GraphQLObjectType createType() {
		Builder root = newObject();
		root.name(USER_TYPE_NAME);
		root.description("Gentics Mesh User");
		interfaceTypeProvider.addCommonFields(root);

		// .username
		root.field(newFieldDefinition().name("username").description("The username of the user").type(GraphQLString).dataFetcher((env) -> {
			HibUser user = env.getSource();
			return user.getUsername();
		}));

		// .firstname
		root.field(newFieldDefinition().name("firstname").description("The firstname of the user").type(GraphQLString).dataFetcher((env) -> {
			HibUser user = env.getSource();
			return user.getFirstname();
		}));

		// .lastname
		root.field(newFieldDefinition().name("lastname").description("The lastname of the user").type(GraphQLString).dataFetcher((env -> {
			HibUser user = env.getSource();
			return user.getLastname();
		})));

		// .emailAddress
		root.field(newFieldDefinition().name("emailAddress").description("The email of the user").type(GraphQLString).dataFetcher((env) -> {
			HibUser user = env.getSource();
			return user.getEmailAddress();
		}));

		// .forcedPasswordChange
		root.field(newFieldDefinition()
			.name("forcedPasswordChange")
			.description("When true, the user needs to change their password on the next login.")
			.type(GraphQLBoolean).dataFetcher((env) -> {
				HibUser user = env.getSource();
				return user.isForcedPasswordChange();
			}));

		// .admin
		root.field(newFieldDefinition()
			.name("admin")
			.description("Flag which indicates whether the user has admin privileges.")
			.type(GraphQLBoolean).dataFetcher((env) -> {
				HibUser user = env.getSource();
				return user.isAdmin();
			}));

		// .groups
		root.field(newPagingFieldWithFetcher("groups", "Groups to which the user belongs.", (env) -> {
			HibUser user = env.getSource();
			GraphQLContext gc = env.getContext();
			UserDao userDao = Tx.get().userDao();

			return userDao.getGroups(user, gc.getUser(), getPagingInfo(env));
		}, GROUP_PAGE_TYPE_NAME));

		// .roles
		root.field(newPagingFieldWithFetcher("roles", "Roles the user has", env -> {
			HibUser user = env.getSource();
			GraphQLContext gc = env.getContext();
			UserDao userDao = Tx.get().userDao();

			return userDao.getRolesViaShortcut(user, gc.getUser(), getPagingInfo(env));
		}, ROLE_PAGE_TYPE_NAME));

		// .rolesHash
		root.field(newFieldDefinition().name("rolesHash").description("Hash of the users roles").type(GraphQLString).dataFetcher((env) -> {
			HibUser user = env.getSource();
			return Tx.get().userDao().getRolesHash(user);
		}));

		// .nodeReference
		root.field(newFieldDefinition()
			.name("nodeReference")
			.description("User node reference")
			.argument(createNodeVersionArg())
			.type(new GraphQLTypeReference("Node"))
			.dataFetcher(env -> {
				Tx tx = Tx.get();
				ContentDao contentDao = tx.contentDao();
				GraphQLContext gc = env.getContext();
				HibUser user = env.getSource();
				HibNode node = (HibNode) user.getReferencedNode();
				if (node == null) {
					return null;
				}
				// Ensure that the graphql traversal does not leave the scope of the current project.
				if (!node.getProject().getUuid().equals(tx.getProject(gc).getUuid())) {
					// TODO throw error - We can't traverse across projects
					return null;
				}

				node = gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
				List<String> languageTags = getLanguageArgument(env);
				ContainerType type = getNodeVersion(env);

				HibNodeFieldContainer container = contentDao.findVersion(node, gc, languageTags, type);
				container = gc.requiresReadPermSoft(container, env);
				return new NodeContent(node, container, languageTags, type);
			}));

		return root.build();
	}

}
