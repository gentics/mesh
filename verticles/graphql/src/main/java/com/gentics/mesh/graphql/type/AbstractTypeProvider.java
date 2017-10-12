package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.error.MeshConfigurationException;
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
import graphql.schema.GraphQLTypeReference;
import rx.functions.Func1;

public abstract class AbstractTypeProvider {

	public static final String LINK_TYPE_NAME = "LinkType";

	public AbstractTypeProvider() {

	}

	/**
	 * Return the elasticsearch query argument.
	 * 
	 * @return
	 */
	public GraphQLArgument createQueryArg() {
		return newArgument().name("query").description("Elasticsearch query to query the data.").type(GraphQLString).build();
	}

	/**
	 * Return a new set of paging arguments.
	 * 
	 * @return
	 */
	public List<GraphQLArgument> createPagingArgs() {
		List<GraphQLArgument> arguments = new ArrayList<>();

		// #page
		arguments.add(newArgument().name("page").defaultValue(1L).description("Page to be selected").type(GraphQLLong).build());

		// #perPage
		arguments.add(newArgument().name("perPage").defaultValue(25).description("Max count of elements per page").type(GraphQLInt).build());
		return arguments;
	}

	public GraphQLArgument createReleaseUuidArg() {
		// #release
		return newArgument().name("release").type(GraphQLString).description("Release Uuid").build();
	}

	/**
	 * Return the lang argument values. The default language will automatically added to the list in order to provide a language fallback.
	 * 
	 * @param env
	 * @return
	 */
	public List<String> getLanguageArgument(DataFetchingEnvironment env) {
		String defaultLanguage = Mesh.mesh().getOptions().getDefaultLanguage();
		List<String> languageTags = new ArrayList<>();
		List<String> argumentList = env.getArgument("lang");
		if (argumentList != null) {
			languageTags.addAll(argumentList);
		}
		// Only use the default language if no other language has been specified.
		if (languageTags.isEmpty()) {
			languageTags.add(defaultLanguage);
		}
		return languageTags;
	}

	/**
	 * Create a new argument for the lang.
	 * 
	 * @return
	 */
	public GraphQLArgument createLanguageTagArg() {

		// #lang
		String defaultLanguage = Mesh.mesh().getOptions().getDefaultLanguage();
		return newArgument().name("lang").type(new GraphQLList(GraphQLString)).description(
				"Language tags to filter by. When set only nodes which contain at least one of the provided language tags will be returned")
				.defaultValue(Arrays.asList(defaultLanguage)).build();
	}

	/**
	 * Return a new argument for the uuid.
	 * 
	 * @param description
	 * @return
	 */
	public GraphQLArgument createUuidArg(String description) {
		return newArgument().name("uuid").type(GraphQLString).description(description).build();
	}

	/**
	 * Return a new webroot path argument.
	 * 
	 * @return
	 */
	public GraphQLArgument createPathArg() {
		return newArgument().name("path").type(GraphQLString).description("Webroot path which points to a container of a node.").build();
	}

	/**
	 * Return a new name argument with the provided description.
	 * 
	 * @param description
	 * @return
	 */
	public GraphQLArgument createNameArg(String description) {
		return newArgument().name("name").type(GraphQLString).description(description).build();
	}

	public GraphQLEnumType createLinkEnumType() {
		GraphQLEnumType linkTypeEnum = newEnum().name(LINK_TYPE_NAME).description("Mesh resolve link type").value(LinkType.FULL.name(), LinkType.FULL,
				"Render full links").value(LinkType.MEDIUM.name(), LinkType.MEDIUM, "Render medium links").value(LinkType.SHORT.name(),
						LinkType.SHORT, "Render short links").value(LinkType.OFF.name(), LinkType.OFF, "Don't render links").build();
		return linkTypeEnum;
	}

	public GraphQLArgument createLinkTypeArg() {

		return newArgument().name("linkType").type(new GraphQLTypeReference(LINK_TYPE_NAME)).defaultValue(LinkType.OFF).description(
				"Specify the resolve type").build();
	}

	/**
	 * Returns the linkType argument value from the given environment.
	 * 
	 * @param env
	 * @return Found value or default value.
	 */
	public LinkType getLinkType(DataFetchingEnvironment env) {
		return env.getArgument("linkType");
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
		MeshCoreVertex<?, ?> element = handleUuidNameArgsNoPerm(env, root::findByUuid, root::findByName);
		if (element == null) {
			return null;
		} else {
			return gc.requiresPerm(element, READ_PERM);
		}
	}

	/**
	 * Handle the UUID or name arguments and locate and return the vertex by the given functions.
	 *
	 * @param env
	 * @param uuidFetcher
	 * @param nameFetcher
	 * @return
	 */
	protected MeshCoreVertex<?, ?> handleUuidNameArgsNoPerm(DataFetchingEnvironment env, Function<String, MeshCoreVertex<?, ?>> uuidFetcher,
			Function<String, MeshCoreVertex<?, ?>> nameFetcher) {
		String uuid = env.getArgument("uuid");
		MeshCoreVertex<?, ?> element = null;
		if (uuid != null) {
			element = uuidFetcher.apply(uuid);
		}
		String name = env.getArgument("name");
		if (name != null) {
			element = nameFetcher.apply(name);
		}
		if (element == null) {
			return null;
		}

		return element;
	}

	protected MeshVertex handleReleaseSchema(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		Release release = env.getSource();
		Stream<? extends SchemaContainerVersion> schemas = StreamSupport.stream(release.findActiveSchemaVersions().spliterator(), false);

		// We need to handle permissions dedicately since we check the schema container perm and not the schema container version perm.
		return handleUuidNameArgsNoPerm(env, uuid -> schemas.filter(schema -> {
			SchemaContainer container = schema.getSchemaContainer();
			return container.getUuid().equals(uuid) && gc.getUser().hasPermission(container, READ_PERM);
		}).findFirst().get(), name -> schemas.filter(schema -> schema.getName().equals(name) && gc.getUser().hasPermission(schema
				.getSchemaContainer(), READ_PERM)).findFirst().get());
	}

	protected Page<SchemaContainerVersion> handleReleaseSchemas(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		Release release = env.getSource();
		Stream<? extends SchemaContainerVersion> schemas = StreamSupport.stream(release.findActiveSchemaVersions().spliterator(), false).filter(
				schema -> gc.getUser().hasPermission(schema.getSchemaContainer(), READ_PERM));
		return new DynamicStreamPageImpl<>(schemas, getPagingInfo(env));
	}

	/**
	 * Construct a field which can page the result set and also accept a elasticsearch query for processing.
	 * 
	 * @param name
	 *            Name of the field
	 * @param description
	 *            Description of the field
	 * @param rootProvider
	 *            Provider of the root element (will only be used when no query was specified)
	 * @param pageTypeName
	 *            Name of the page type
	 * @param indexHandler
	 *            Handler which will be used to invoke the query
	 * @return
	 */
	protected GraphQLFieldDefinition newPagingSearchField(String name, String description, Func1<GraphQLContext, RootVertex<?>> rootProvider,
			String pageTypeName, IndexHandler<?> indexHandler) {
		return newFieldDefinition().name(name).description(description).argument(createPagingArgs()).argument(createQueryArg()).type(
				new GraphQLTypeReference(pageTypeName)).dataFetcher((env) -> {
					GraphQLContext gc = env.getContext();
					String query = env.getArgument("query");
					if (query != null) {
						try {
							return indexHandler.query(gc, query, getPagingInfo(env), READ_PERM);
						} catch (MeshConfigurationException | InterruptedException | ExecutionException | TimeoutException e) {
							throw new RuntimeException(e);
						}
					} else {
						return rootProvider.call(gc).findAll(gc, getPagingInfo(env));
					}
				}).build();
	}

	/**
	 * Construct a new paging field which fetches specific data.
	 * 
	 * @param name
	 *            Name of the field
	 * @param description
	 *            Description of the field
	 * @param dataFetcher
	 *            Data fetcher to be used
	 * @param referenceTypeName
	 *            Type of objects which the field yields
	 * @return Field definition
	 */
	protected GraphQLFieldDefinition newPagingFieldWithFetcher(String name, String description, DataFetcher<?> dataFetcher,
			String referenceTypeName) {
		return newPagingFieldWithFetcherBuilder(name, description, dataFetcher, referenceTypeName).build();
	}

	protected graphql.schema.GraphQLFieldDefinition.Builder newPagingFieldWithFetcherBuilder(String name, String description,
			DataFetcher<?> dataFetcher, String pageTypeName) {
		return newFieldDefinition().name(name).description(description).argument(createPagingArgs()).type(new GraphQLTypeReference(pageTypeName))
				.dataFetcher(dataFetcher);
	}

	protected GraphQLFieldDefinition newPagingField(String name, String description, Func1<GraphQLContext, RootVertex<?>> rootProvider,
			String referenceTypeName) {
		return newPagingFieldWithFetcher(name, description, (env) -> {
			GraphQLContext gc = env.getContext();
			return rootProvider.call(gc).findAll(gc, getPagingInfo(env));
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
	 * @param elementType
	 *            Type name of the element which can be loaded
	 * @return
	 */
	protected GraphQLFieldDefinition newElementField(String name, String description, Func1<GraphQLContext, RootVertex<?>> rootProvider,
			String elementType) {
		return newFieldDefinition().name(name).description(description).argument(createUuidArg("Uuid of the " + name + ".")).argument(createNameArg(
				"Name of the " + name + ".")).type(new GraphQLTypeReference(elementType)).dataFetcher(env -> {
					GraphQLContext gc = env.getContext();
					return handleUuidNameArgs(env, rootProvider.call(gc));
				}).build();
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
