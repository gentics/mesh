package com.gentics.mesh.graphql.type.argument;

import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLArgument;

@Singleton
public class ArgumentsProvider {

	@Inject
	public ArgumentsProvider() {
	}

	public List<GraphQLArgument> getPagingArgs() {
		List<GraphQLArgument> arguments = new ArrayList<>();
		arguments.add(newArgument().name("page")
				.defaultValue(1)
				.description("Page to be selected")
				.type(GraphQLLong)
				.build());
		arguments.add(newArgument().name("perPage")
				.defaultValue(25)
				.description("Max count of elements per page")
				.type(GraphQLLong)
				.build());
		return arguments;
	}

	public GraphQLArgument getUuidArg(String description) {
		return newArgument().name("uuid")
				.type(GraphQLString)
				.description(description)
				.build();
	}

	public GraphQLArgument getPathArg() {
		return newArgument().name("path")
				.type(GraphQLString)
				.description("Node webroot path")
				.build();
	}
}
