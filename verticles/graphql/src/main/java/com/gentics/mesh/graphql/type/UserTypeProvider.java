package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.graphql.type.GroupTypeProvider.GROUP_PAGE_TYPE_NAME;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.Map;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.tx.Tx;

import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;
import io.vertx.core.json.JsonObject;

@Singleton
public class UserTypeProvider extends AbstractTypeProvider implements Filterable {

	public static final String USER_TYPE_NAME = "User";

	public static final String USER_PAGE_TYPE_NAME = "UsersPage";

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public UserTypeProvider() {
	}

	public GraphQLObjectType createType() {
		Builder root = newObject();
		root.name(USER_TYPE_NAME);
		root.description("Gentics Mesh User");
		interfaceTypeProvider.addCommonFields(root);

		// .username
		root.field(newFieldDefinition().name("username").description("The username of the user").type(GraphQLString).dataFetcher((env) -> {
			User user = env.getSource();
			return user.getUsername();
		}));

		// .firstname
		root.field(newFieldDefinition().name("firstname").description("The firstname of the user").type(GraphQLString).dataFetcher((env) -> {
			User user = env.getSource();
			return user.getFirstname();
		}));

		// .lastname
		root.field(newFieldDefinition().name("lastname").description("The lastname of the user").type(GraphQLString).dataFetcher((env -> {
			User user = env.getSource();
			return user.getLastname();
		})));

		// .emailAddress
		root.field(newFieldDefinition().name("emailAddress").description("The email of the user").type(GraphQLString).dataFetcher((env) -> {
			User user = env.getSource();
			return user.getEmailAddress();
		}));

		// .groups
		root.field(newPagingFieldWithFetcher("groups", "Groups to which the user belongs.", (env) -> {
			User user = env.getSource();
			GraphQLContext gc = env.getContext();
			return user.getGroups(gc.getUser(), getPagingInfo(env));
		}, GROUP_PAGE_TYPE_NAME));

		// .nodeReference
		root.field(newFieldDefinition().name("nodeReference").description("User node reference").type(new GraphQLTypeReference("Node"))
			.dataFetcher((env) -> {
				GraphQLContext gc = env.getContext();
				User user = env.getSource();
				Node node = user.getReferencedNode();
				if (node == null) {
					return null;
				}
				// Ensure that the graphql traversal does not leave the scope of the current project.
				if (!node.getProject().getUuid().equals(gc.getProject().getUuid())) {
					// TODO throw error - We can't traverse across projects
					return null;
				}
				return gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
			}));

		return root.build();
	}

	@Override
	public GraphQLArgument createFilterArgument() {

		// eq()
		GraphQLInputObjectField eq = GraphQLInputObjectField.newInputObjectField().name("eq").description("Filter by quality of the string.")
			.type(GraphQLString).build();

		GraphQLInputObjectType usernameType = GraphQLInputObjectType.newInputObject().name("username").field(eq)
			.description("Filter by username")
			.build();

		GraphQLInputObjectField firstname = GraphQLInputObjectField.newInputObjectField().name("firstname").type(GraphQLString)
			.description("Filter by firstname")
			.build();

		GraphQLInputObjectField lastname = GraphQLInputObjectField.newInputObjectField().name("lastname").type(GraphQLString)
			.description("Filter by lastname")
			.build();

		graphql.schema.GraphQLInputObjectType.Builder filterType = GraphQLInputObjectType.newInputObject().name("userFilter")

			// .username
			.field(GraphQLInputObjectField.newInputObjectField().name("username").type(usernameType))

			// .firstnam
			.field(firstname)

			// .lastname
			.field(lastname);

		return newArgument().name("filter").type(filterType.build()).description("Specify a filter").build();
	}

	@Override
	public Predicate<Vertex> constructFilter(Map<String, Object> filter, RootVertex<?> root) {
		if (filter == null) {
			return null;
		} else {
			FramedGraph graph = Tx.getActive().getGraph();
			JsonObject json = new JsonObject(filter);
			return (v) -> {
				User user = (User) graph.frameElementExplicit(v, root.getPersistanceClass());
				JsonObject usernameFilter = json.getJsonObject("username");
				if (usernameFilter != null) {
					String eq = usernameFilter.getString("eq");
					if (eq != null) {
						return eq.equals(user.getUsername());
					}
				}
				return true;
			};
		}
	}

}
