package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.User;

import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class UserTypeProvider extends AbstractTypeProvider{

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public UserTypeProvider() {
	}

	public GraphQLObjectType getUserType() {
		Builder root = newObject();
		root.name("User");
		root.description("User description");
		interfaceTypeProvider.addCommonFields(root);
		root.field(newFieldDefinition().name("username")
				.description("The username of the user")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof User) {
						return ((User) source).getUsername();
					}
					return null;
				})
				.build());

		root.field(newFieldDefinition().name("firstname")
				.description("The firstname of the user")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof User) {
						return ((User) source).getFirstname();
					}
					return null;
				})
				.build());

		root.field(newFieldDefinition().name("lastname")
				.description("The lastname of the user")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof User) {
						return ((User) source).getLastname();
					}
					return null;
				})
				.build());

		root.field(newFieldDefinition().name("emailAddress")
				.description("The email of the user")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof User) {
						return ((User) source).getEmailAddress();
					}
					return null;
				})
				.build());

		root.field(newFieldDefinition().name("groups")
				.description("Groups to which the user belongs")
				.argument(getPagingArgs())
				.type(new GraphQLList(new GraphQLTypeReference("Group")))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof User) {
						return ((User) source).getGroups();
					}
					return null;
				})
				.build());

		root.field(newFieldDefinition().name("nodeReference")
				.description("User node reference")
				.type(new GraphQLTypeReference("Node"))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof User) {
						return ((User) source).getReferencedNode();
					}
					return null;
				})
				.build());
		return root.build();
	}
}
