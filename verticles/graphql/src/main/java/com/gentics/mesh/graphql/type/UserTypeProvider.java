package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class UserTypeProvider extends AbstractTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public UserTypeProvider() {
	}

	public Object nodeReferenceFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof User) {
			InternalActionContext ac = (InternalActionContext) env.getContext();
			Node node = ((User) source).getReferencedNode();
			if (node != null) {
				if (ac.getUser()
						.hasPermission(node, GraphPermission.READ_PERM)
						|| ac.getUser()
								.hasPermission(node, GraphPermission.READ_PUBLISHED_PERM)) {
					return node;
				}
			}
		}
		return null;
	}

	public Object groupsFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof User) {
			//TODO handle perms
			return ((User) source).getGroups();
		}
		return null;
	}

	public Object usernameFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof User) {
			return ((User) source).getUsername();
		}
		return null;
	}

	public Object firstnameFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof User) {
			return ((User) source).getFirstname();
		}
		return null;
	}

	public Object lastnameFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof User) {
			return ((User) source).getLastname();
		}
		return null;
	}

	public Object emailAddressFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof User) {
			return ((User) source).getEmailAddress();
		}
		return null;
	}

	public GraphQLObjectType getUserType() {
		Builder root = newObject();
		root.name("User");
		root.description("User description");
		interfaceTypeProvider.addCommonFields(root);
		root.field(newFieldDefinition().name("username")
				.description("The username of the user")
				.type(GraphQLString)
				.dataFetcher(this::usernameFetcher)
				.build());

		root.field(newFieldDefinition().name("firstname")
				.description("The firstname of the user")
				.type(GraphQLString)
				.dataFetcher(this::firstnameFetcher)
				.build());

		root.field(newFieldDefinition().name("lastname")
				.description("The lastname of the user")
				.type(GraphQLString)
				.dataFetcher(this::lastnameFetcher)
				.build());

		root.field(newFieldDefinition().name("emailAddress")
				.description("The email of the user")
				.type(GraphQLString)
				.dataFetcher(this::emailAddressFetcher)
				.build());

		root.field(newFieldDefinition().name("groups")
				.description("Groups to which the user belongs")
				.argument(getPagingArgs())
				.type(new GraphQLList(new GraphQLTypeReference("Group")))
				.dataFetcher(this::groupsFetcher)
				.build());

		root.field(newFieldDefinition().name("nodeReference")
				.description("User node reference")
				.type(new GraphQLTypeReference("Node"))
				.dataFetcher(this::nodeReferenceFetcher)
				.build());
		return root.build();
	}
}
