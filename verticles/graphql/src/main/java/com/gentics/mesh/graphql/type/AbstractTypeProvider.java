package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.WordUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import rx.functions.Func1;

public abstract class AbstractTypeProvider {

	/**
	 * Return a new set of paging arguments.
	 * 
	 * @return
	 */
	public List<GraphQLArgument> getPagingArgs() {
		List<GraphQLArgument> arguments = new ArrayList<>();

		// .page
		arguments.add(newArgument().name("page")
				.defaultValue(1L)
				.description("Page to be selected")
				.type(GraphQLLong)
				.build());

		// .perPage
		arguments.add(newArgument().name("perPage")
				.defaultValue(25)
				.description("Max count of elements per page")
				.type(GraphQLInt)
				.build());
		return arguments;
	}

	public GraphQLArgument getReleaseUuidArg() {
		return newArgument().name("release")
				.type(GraphQLString)
				.description("Release Uuid")
				.build();
	}

	public GraphQLArgument getLanguageTagArg() {
		return newArgument().name("language")
				.type(GraphQLString)
				.description("Language tag")
				.defaultValue("en")
				.build();
	}

	public GraphQLArgument getLanguageTagListArg() {
		return newArgument().name("languages")
				.type(new GraphQLList(GraphQLString))
				.description(
						"Language tags to filter by. When set only nodes which contain at least one of the provided language tags will be returned")
				.build();
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

	protected MeshVertex handleUuidNameArgs(DataFetchingEnvironment env, RootVertex<?> root) {
		String uuid = env.getArgument("uuid");
		MeshVertex element = null;
		if (uuid != null) {
			element = root.findByUuid(uuid);
		}
		String name = env.getArgument("name");
		if (name != null) {
			element = root.findByName(name);
		}
		if (element == null) {
			return null;
		}
		InternalActionContext ac = env.getContext();
		if (ac.getUser()
				.hasPermission(element, GraphPermission.READ_PERM)) {
			return element;
		}
		return element;
	}

	/**
	 * Construct a page type with the given element type for its nested elements.
	 * 
	 * @param name
	 *            Name of the element that is being nested
	 * @param elementType
	 *            Type of the nested element
	 * @return
	 */
	protected GraphQLObjectType newPageType(String name, GraphQLType elementType) {
		Builder type = newObject().name("Page" + WordUtils.capitalize(name))
				.description("Paged result");
		type.field(newFieldDefinition().name("elements")
				.type(new GraphQLList(elementType))
				.dataFetcher(env -> {
					Page<?> page = env.getSource();
					return page;
				}));

		type.field(newFieldDefinition().name("totalElements")
				.description("Return the total item count which the resource could provide.")
				.dataFetcher(env -> {
					Page<?> page = env.getSource();
					return page.getTotalElements();
				})
				.type(GraphQLLong));

		type.field(newFieldDefinition().name("number")
				.description("Return the page number of the page.")
				.dataFetcher(env -> {
					Page<?> page = env.getSource();
					return page.getNumber();
				})
				.type(GraphQLLong));

		type.field(newFieldDefinition().name("totalPages")
				.description("Return the total amount of pages which the resource can provide.")
				.dataFetcher(env -> {
					Page<?> page = env.getSource();
					return page.getTotalPages();
				})
				.type(GraphQLLong));

		type.field(newFieldDefinition().name("perPage")
				.description("Return the per page parameter value that was used to load the page.")
				.dataFetcher(env -> {
					Object source = env.getSource();
					if (source instanceof Page) {
						return ((Page<?>) source).getPerPage();
					}
					return null;
				})
				.type(GraphQLLong));

		type.field(newFieldDefinition().name("count")
				.description(
						"Return the amount of items which the page is containing. Please note that a page may always contain less items compared to its maximum capacity.")
				.dataFetcher(env -> {
					Object source = env.getSource();
					if (source instanceof Page) {
						return ((Page<?>) source).getNumberOfElements();
					}
					return null;
				})
				.type(GraphQLLong));

		type.field(newFieldDefinition().name("hasNextPage")
				.description("Check whether the paged resource could serve another page")
				.type(GraphQLBoolean)
				.dataFetcher(env -> {
					Page<?> page = env.getSource();
					return page.getTotalPages() > page.getNumber();
				}));

		type.field(newFieldDefinition().name("hasPreviousPage")
				.description("Check whether the current page has a previous page.")
				.type(GraphQLBoolean)
				.dataFetcher(env -> {
					Page<?> page = env.getSource();
					return page.getNumber() > 1;
				}));

		type.field(newFieldDefinition().name("size")
				.description("Return the amount of elements which the page can hold.")
				.dataFetcher(env -> {
					Page<?> page = env.getSource();
					return page.getSize();
				})
				.type(GraphQLInt));

		return type.build();
	}

	protected GraphQLFieldDefinition newPagingFieldWithFetcher(String name, String description, DataFetcher dataFetcher, String referenceTypeName) {
		return newPagingFieldWithFetcherBuilder(name, description, dataFetcher, referenceTypeName).build();
	}
	
	protected graphql.schema.GraphQLFieldDefinition.Builder newPagingFieldWithFetcherBuilder(String name, String description, DataFetcher dataFetcher, String referenceTypeName) {
		return newFieldDefinition().name(name)
				.description(description)
				.argument(getPagingArgs())
				.type(newPageType(name, new GraphQLTypeReference(referenceTypeName)))
				.dataFetcher(dataFetcher);
	}

	protected GraphQLFieldDefinition newPagingField(String name, String description, Func1<InternalActionContext, RootVertex<?>> rootProvider,
			String referenceTypeName) {
		return newPagingFieldWithFetcher(name, description, (env) -> {
			Object source = env.getSource();
			if (source instanceof InternalActionContext) {
				InternalActionContext ac = (InternalActionContext) source;
				return rootProvider.call(ac)
						.findAll(ac, getPagingInfo(env));
			}
			return null;
		}, referenceTypeName);
	}

	protected GraphQLFieldDefinition newElementField(String name, String description, Func1<InternalActionContext, RootVertex<?>> rootProvider,
			GraphQLObjectType type) {
		return newFieldDefinition().name(name)
				.description(description)
				.argument(getUuidArg("Uuid of the " + name + "."))
				.argument(getNameArg("Name of the " + name + "."))
				.type(type)
				.dataFetcher(env -> {
					Object source = env.getSource();
					if (source instanceof InternalActionContext) {
						InternalActionContext ac = (InternalActionContext) source;
						return handleUuidNameArgs(env, rootProvider.call(ac));
					}
					return null;
				})
				.build();
	}

	/**
	 * Load the paging parameters from the provided {@link DataFetchingEnvironment}.
	 * 
	 * @param env
	 * @return Loaded paging parameters
	 */
	protected PagingParameters getPagingInfo(DataFetchingEnvironment env) {
		PagingParameters parameters = new PagingParametersImpl();
		Long page = env.getArgument("page");
		if (page != null) {
			parameters.setPage(page);
		}
		Integer perPage = env.getArgument("perPage");
		if (perPage != null) {
			parameters.setPerPage(perPage);
		}
		parameters.validate();
		return parameters;
	}

}
