package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
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

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.graphql.context.GraphQLContext;
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
		// .release
		return newArgument().name("release")
				.type(GraphQLString)
				.description("Release Uuid")
				.build();
	}

	public GraphQLArgument getLanguageTagArg() {
		// .language
		String defaultLanguage = Mesh.mesh()
				.getOptions()
				.getDefaultLanguage();
		return newArgument().name("language")
				.type(GraphQLString)
				.description("Language tag")
				.defaultValue(defaultLanguage)
				.build();
	}

	public GraphQLArgument createLanguageTagListArg() {
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
	public GraphQLArgument createUuidArg(String description) {
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
	public GraphQLArgument createPathArg() {
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
	public GraphQLArgument createNameArg(String description) {
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
	public PagingParameters createPagingParameters(DataFetchingEnvironment env) {
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

	public GraphQLArgument createLinkTypeArg() {

		GraphQLEnumType linkTypeEnum = newEnum().name("LinkType")
				.description("Mesh resolve link type")
				.value(LinkType.FULL.name(), LinkType.FULL, "Render full links")
				.value(LinkType.MEDIUM.name(), LinkType.MEDIUM, "Render medium links")
				.value(LinkType.SHORT.name(), LinkType.SHORT, "Render short links")
				.value(LinkType.OFF.name(), LinkType.OFF, "Don't render links")
				.build();

		return newArgument().name("linkType")
				.type(linkTypeEnum)
				.defaultValue(LinkType.OFF)
				.description("Specify the resolve type")
				.build();
	}

	/**
	 * Handle the UUID or name arguments and locate and return the vertex from the root vertex.
	 * 
	 * @param env
	 * @param root
	 * @return
	 */
	protected MeshVertex handleUuidNameArgs(DataFetchingEnvironment env, RootVertex<?> root) {
		GraphQLContext gc = env.getContext();
		String uuid = env.getArgument("uuid");
		MeshCoreVertex<?, ?> element = null;
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

		// Check permissions
		return gc.requiresPerm(element, READ_PERM);
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
					return env.getSource();
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
					Page<?> page = env.getSource();
					return page.getPerPage();
				})
				.type(GraphQLLong));

		type.field(newFieldDefinition().name("count")
				.description(
						"Return the amount of items which the page is containing. Please note that a page may always contain less items compared to its maximum capacity.")
				.dataFetcher(env -> {
					Page<?> page = env.getSource();
					return page.getNumberOfElements();
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

	protected GraphQLFieldDefinition newPagingFieldWithFetcher(String name, String description, DataFetcher<?> dataFetcher,
			String referenceTypeName) {
		return newPagingFieldWithFetcherBuilder(name, description, dataFetcher, referenceTypeName).build();
	}

	protected graphql.schema.GraphQLFieldDefinition.Builder newPagingFieldWithFetcherBuilder(String name, String description,
			DataFetcher<?> dataFetcher, String referenceTypeName) {
		return newFieldDefinition().name(name)
				.description(description)
				.argument(getPagingArgs())
				.type(newPageType(name, new GraphQLTypeReference(referenceTypeName)))
				.dataFetcher(dataFetcher);
	}

	protected GraphQLFieldDefinition newPagingField(String name, String description, Func1<GraphQLContext, RootVertex<?>> rootProvider,
			String referenceTypeName) {
		return newPagingFieldWithFetcher(name, description, (env) -> {
			GraphQLContext gc = env.getContext();
			return rootProvider.call(gc)
					.findAll(gc, getPagingInfo(env));
		}, referenceTypeName);
	}

	/**
	 * Create a new elements field which automatically allows to resolve the element using it's name or uuid.
	 * 
	 * @param name
	 *            Name of the field
	 * @param description
	 *            Description of the field
	 * @param rootProvider
	 *            Function which will return the root vertex which is used to load the element
	 * @param type
	 *            Type of the element which can be loaded
	 * @return
	 */
	protected GraphQLFieldDefinition newElementField(String name, String description, Func1<GraphQLContext, RootVertex<?>> rootProvider,
			GraphQLObjectType type) {
		return newFieldDefinition().name(name)
				.description(description)
				.argument(createUuidArg("Uuid of the " + name + "."))
				.argument(createNameArg("Name of the " + name + "."))
				.type(type)
				.dataFetcher(env -> {
					GraphQLContext gc = env.getContext();
					return handleUuidNameArgs(env, rootProvider.call(gc));
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
