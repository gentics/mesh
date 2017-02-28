package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLEnumType.newEnum;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;

public abstract class AbstractTypeProvider {

	/**
	 * Return a new set of paging arguments.
	 * 
	 * @return
	 */
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
				.type(GraphQLInt)
				.build());
		return arguments;
	}

	/**
	 * Return a new argument for the uuid.
	 * 
	 * @param description
	 * @return
	 */
	public GraphQLArgument getUuidArg(String description) {
		return newArgument().name("uuid")
				.type(GraphQLString)
				.description(description)
				.build();
	}

	/**
	 * Return a new webroot path argument.
	 * 
	 * @return
	 */
	public GraphQLArgument getPathArg() {
		return newArgument().name("path")
				.type(GraphQLString)
				.description("Node webroot path")
				.build();
	}

	/**
	 * Return a new name argument with the provided description.
	 * 
	 * @param description
	 * @return
	 */
	public GraphQLArgument getNameArg(String description) {
		return newArgument().name("name")
				.type(GraphQLString)
				.description(description)
				.build();
	}

	/**
	 * Load the paging parameters from the environment arguments.
	 * 
	 * @param fetcher
	 * @return
	 */
	public PagingParameters getPagingParameters(DataFetchingEnvironment env) {
		PagingParameters params = new PagingParametersImpl();
		Long page = env.getArgument("page");
		if (page != null) {
			params.setPage(page);
		}
		Integer perPage = env.getArgument("perPage");
		if (perPage != null) {
			params.setPerPage(perPage);
		}
		return params;
	}

	public GraphQLArgument getLinkTypeArg() {

		GraphQLEnumType linkTypeEnum = newEnum().name("LinkType")
				.description("Mesh resolve link type")
				.value(LinkType.FULL.name(), LinkType.FULL, "Render full links")
				.value(LinkType.MEDIUM.name(), LinkType.MEDIUM, "Render medium links")
				.value(LinkType.SHORT.name(), LinkType.SHORT, "Render short links")
				.value(LinkType.OFF.name(), LinkType.OFF, "Don't render links")
				.build();

		return newArgument().name("linkType")
				.type(linkTypeEnum)
				.defaultValue(LinkType.OFF.name())
				.description("Specify the resolve type")
				.build();
	}

}
