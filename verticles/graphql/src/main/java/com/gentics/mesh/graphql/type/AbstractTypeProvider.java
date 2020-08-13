package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.action.DAOActionContext.context;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.graphqlfilter.filter.StartFilter;
import com.gentics.mesh.core.action.DAOActions;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.PermissionException;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.NodeFilter;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.search.SearchHandler;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldDefinition.Builder;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLTypeReference;

public abstract class AbstractTypeProvider {

	public static final String LINK_TYPE_NAME = "LinkType";
	public static final String NODE_CONTAINER_VERSION_NAME = "NodeVersion";

	private final MeshOptions options;

	public AbstractTypeProvider(MeshOptions options) {
		this.options = options;
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
		arguments.add(newArgument().name("perPage").description("Max count of elements per page").type(GraphQLLong).build());
		return arguments;
	}

	public GraphQLArgument createBranchUuidArg() {
		// #branch
		return newArgument().name("branch").type(GraphQLString).description("Branch Uuid").build();
	}

	public List<String> getLanguageArgument(DataFetchingEnvironment env) {
		return getLanguageArgument(env, (List<String>) null);
	}

	public String getSingleLanguageArgument(DataFetchingEnvironment env) {
		String argument = env.getArgument("lang");
		if (argument != null) {
			return argument;
		} else {
			return options.getDefaultLanguage();
		}
	}

	/**
	 * Generate a language fallback list and utilize any existing language fallback list from the given content.
	 * 
	 * @param env
	 * @param content
	 * @return
	 */
	public List<String> getLanguageArgument(DataFetchingEnvironment env, NodeContent content) {
		return getLanguageArgument(env, content.getLanguageFallback());
	}

	/**
	 * Generate a language fallback list and utilize the given container language. Prefer the language of the container for the fallback.
	 * 
	 * @param env
	 * @param source
	 * @return
	 */
	public List<String> getLanguageArgument(DataFetchingEnvironment env, GraphFieldContainer source) {
		return getLanguageArgument(env, Arrays.asList(source.getLanguageTag()));
	}

	/**
	 * Return the lang argument values. The default language will automatically added to the list in order to provide a language fallback.
	 * 
	 * @param env
	 * @param preferedLanguages
	 * @return
	 */
	public List<String> getLanguageArgument(DataFetchingEnvironment env, List<String> preferedLanguages) {
		String defaultLanguage = options.getDefaultLanguage();
		List<String> languageTags = new ArrayList<>();

		// 1. Any manual specified fallback is preferred
		List<String> argumentList = env.getArgument("lang");
		if (argumentList != null) {
			languageTags.addAll(argumentList);
		}
		// 2. Append any other preferred languages (e.g. languages from previous fallbacks)
		if (preferedLanguages != null) {
			languageTags.addAll(preferedLanguages);
		}
		// 3. Only use the default language if no other language has been specified.
		if (languageTags.isEmpty()) {
			languageTags.add(defaultLanguage);
		}
		return languageTags;
	}

	/**
	 * Create a new argument for the lang.
	 * 
	 * @param withDefaultLang
	 * @return
	 */
	public GraphQLArgument createLanguageTagArg(boolean withDefaultLang) {

		// #lang
		String defaultLanguage = options.getDefaultLanguage();
		graphql.schema.GraphQLArgument.Builder arg = newArgument()
			.name("lang")
			.type(new GraphQLList(GraphQLString))
			.description("Language tags to filter by. When set only nodes which contain at least one of the provided language tags will be returned");

		if (withDefaultLang) {
			arg.defaultValue(Arrays.asList(defaultLanguage));
		}

		return arg.build();
	}

	public GraphQLArgument createSingleLanguageTagArg(boolean withDefaultLang) {

		// #lang
		String defaultLanguage = options.getDefaultLanguage();
		graphql.schema.GraphQLArgument.Builder arg = newArgument()
			.name("lang")
			.type(GraphQLString)
			.description("Language tag to filter by.");

		if (withDefaultLang) {
			arg.defaultValue(defaultLanguage);
		}

		return arg.build();
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
	 * Return a new argument for a list of uuids.
	 *
	 * @param description
	 *            The new arguments description
	 * @return A new argument for a list of uuids
	 */
	public GraphQLArgument createUuidsArg(String description) {
		return newArgument().name("uuids").type(GraphQLList.list(GraphQLString)).description(description).build();
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
				LinkType.SHORT, "Render short links")
			.value(LinkType.OFF.name(), LinkType.OFF, "Don't render links").build();
		return linkTypeEnum;
	}

	public GraphQLArgument createLinkTypeArg() {
		return newArgument().name("linkType").type(new GraphQLTypeReference(LINK_TYPE_NAME)).defaultValue(LinkType.OFF).description(
			"Specify the resolve type").build();
	}

	public GraphQLArgument createNodeVersionArg() {
		return newArgument()
			.name("version")
			.type(new GraphQLTypeReference(NODE_CONTAINER_VERSION_NAME))
			.description("The version of the content which can either be draft or published.")
			.build();
	}

	public GraphQLEnumType createNodeEnumType() {
		GraphQLEnumType nodeVersionEnum = newEnum().name(NODE_CONTAINER_VERSION_NAME)
			.description("The version of a node which can either be published or draft.")
			.value(ContainerType.DRAFT.getHumanCode(), ContainerType.DRAFT, "Draft nodes")
			.value(ContainerType.PUBLISHED.getHumanCode(), ContainerType.PUBLISHED, "Published nodes")
			.build();
		return nodeVersionEnum;
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
	 * Return the node version argument value.
	 *
	 * @param env
	 * @return
	 */
	public ContainerType getNodeVersion(DataFetchingEnvironment env) {
		GraphQLContext context = env.getContext();
		ContainerType type = env.getArgument("version");
		if (type == null) {
			Object source = env.getSource();
			if (source == null) {
				return getDefaultNodeVersion(context);
			} else {
				if (source instanceof NodeContent) {
					NodeContent content = (NodeContent) source;
					ContainerType contentType = content.getType();
					if (contentType != null) {
						return contentType;
					}
				}
				return getDefaultNodeVersion(context);
			}
		} else {
			return type;
		}
	}

	private ContainerType getDefaultNodeVersion(GraphQLContext context) {
		// Utilize the query parameter value here to override the default.
		// This is a fallback mechanism
		VersioningParameters params = context.getVersioningParameters();
		if (params.hasVersion()) {
			String version = params.getVersion();
			return ContainerType.forVersion(version);
		} else {
			return ContainerType.DRAFT;
		}
	}

	/**
	 * Handle the UUID or name arguments and locate and return the vertex from the root vertex.
	 * 
	 * @param env
	 * @param parent
	 * @param root
	 * @return
	 */
	protected HibCoreElement handleUuidNameArgs(DataFetchingEnvironment env, Object parent, DAOActions<?, ?> actions) {
		GraphQLContext gc = env.getContext();
		HibCoreElement element = handleUuidNameArgsNoPerm(env, uuid -> actions.loadByUuid(context(Tx.get(), gc, parent), uuid, null, false),
			name -> actions.loadByName(context(Tx.get(), gc), name, null, false));
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
	protected HibCoreElement handleUuidNameArgsNoPerm(DataFetchingEnvironment env, Function<String, HibCoreElement> uuidFetcher,
		Function<String, HibCoreElement> nameFetcher) {
		String uuid = env.getArgument("uuid");
		HibCoreElement element = null;
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

	protected HibElement handleBranchSchema(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		Branch branch = env.getSource();
		Stream<? extends SchemaVersion> schemas = StreamSupport.stream(branch.findActiveSchemaVersions().spliterator(), false);
		UserDaoWrapper userDao = Tx.get().data().userDao();

		// We need to handle permissions dedicately since we check the schema container perm and not the schema container version perm.
		return handleUuidNameArgsNoPerm(env, uuid -> schemas.filter(schema -> {
			Schema container = schema.getSchemaContainer();
			return container.getUuid().equals(uuid) && userDao.hasPermission(gc.getUser(), container, READ_PERM);
		}).findFirst().get(), name -> schemas.filter(schema -> schema.getName().equals(name) && userDao.hasPermission(gc.getUser(), schema
			.getSchemaContainer(), READ_PERM)).findFirst().get());
	}

	protected Page<SchemaVersion> handleBranchSchemas(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		Branch branch = env.getSource();
		UserDaoWrapper userDao = Tx.get().data().userDao();

		Stream<? extends SchemaVersion> schemas = StreamSupport.stream(branch.findActiveSchemaVersions().spliterator(), false).filter(
			schema -> userDao.hasPermission(gc.getUser(), schema.getSchemaContainer(), READ_PERM));
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
	 * @param searchHandler
	 *            Handler which will be used to invoke the query
	 * @param filterProvider
	 * @return
	 */
	protected <T extends HibCoreElement> GraphQLFieldDefinition newPagingSearchField(String name, String description,
		DAOActions<T, ?> actions,
		String pageTypeName, SearchHandler<?, ?> searchHandler, StartFilter<T, Map<String, ?>> filterProvider) {
		Builder fieldDefBuilder = newFieldDefinition()
			.name(name)
			.description(description)
			.argument(createPagingArgs())
			.argument(createQueryArg()).type(new GraphQLTypeReference(pageTypeName))
			.dataFetcher((env) -> {
				GraphQLContext gc = env.getContext();
				String query = env.getArgument("query");
				Map<String, Object> filter = env.getArgument("filter");
				if (query != null && filter != null) {
					throw new RuntimeException("Only one way of filtering can be specified. Either by query or by filter");
				}
				if (query != null) {
					try {
						return searchHandler.query(gc, query, getPagingInfo(env), READ_PERM);
					} catch (MeshConfigurationException | InterruptedException | ExecutionException | TimeoutException e) {
						throw new RuntimeException(e);
					}
				} else {
					if (filterProvider != null && filter != null) {
						return actions.loadAll(context(Tx.get(), gc, env.getSource()), getPagingInfo(env), filterProvider.createPredicate(filter));
					} else {
						return actions.loadAll(context(Tx.get(), gc, env.getSource()), getPagingInfo(env));
					}
				}
			});

		if (filterProvider != null) {
			fieldDefBuilder.argument(filterProvider.createFilterArgument());
		}
		return fieldDefBuilder.build();
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
		return newFieldDefinition().name(name).description(description)
			.argument(createPagingArgs())
			.argument(createNodeVersionArg())
			.type(new GraphQLTypeReference(pageTypeName))
			.dataFetcher(dataFetcher);
	}

	protected GraphQLFieldDefinition newPagingField(String name, String description, DAOActions<?, ?> actions,
		String referenceTypeName) {
		return newPagingFieldWithFetcher(name, description, env -> {
			GraphQLContext gc = env.getContext();
			return actions.loadAll(context(Tx.get(), gc, env.getSource()), getPagingInfo(env));
		}, referenceTypeName);
	}

	/**
	 * Create a new elements field which automatically allows to resolve the element using it's name or uuid.
	 * 
	 * @param name
	 *            Name of the field
	 * @param description
	 *            Description of the field
	 * @param actions
	 *            DAO actions for the type of elements
	 * @param elementType
	 *            Type name of the element which can be loaded
	 * @return
	 */
	protected GraphQLFieldDefinition newElementField(String name, String description, DAOActions<?, ?> actions,
		String elementType) {
		return newElementField(name, description, actions, elementType, false);
	}

	/**
	 * Create a new elements field which automatically allows to resolve the element using it's name or uuid.
	 * 
	 * @param name
	 *            Name of the field
	 * @param description
	 *            Description of the field
	 * @param actions
	 *            DAO Actions for the type of elements
	 * @param elementType
	 *            Type name of the element which can be loaded
	 * @param hidePermissionsErrors
	 *            Does not show errors if the permission is missing. Useful for sensitive data (ex. fetching users by name)
	 * @return
	 */
	protected GraphQLFieldDefinition newElementField(String name, String description, DAOActions<?, ?> actions,
		String elementType, boolean hidePermissionsErrors) {
		return newFieldDefinition()
			.name(name)
			.description(description)
			.argument(createUuidArg("Uuid of the " + name + "."))
			.argument(createNameArg("Name of the " + name + "."))
			.type(new GraphQLTypeReference(elementType)).dataFetcher(env -> {
				GraphQLContext gc = env.getContext();
				if (hidePermissionsErrors) {
					try {
						return handleUuidNameArgs(env, null, actions);
					} catch (PermissionException e) {
						return null;
					}
				} else {
					return handleUuidNameArgs(env, null, actions);
				}
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
		Long perPage = env.getArgument("perPage");
		if (perPage != null) {
			parameters.setPerPage(perPage);
		}
		parameters.validate();
		return parameters;
	}

	/**
	 * Fetches nodes and applies filters
	 *
	 * @param env
	 *            the environment of the request
	 * @return the filtered nodes
	 */
	protected DynamicStreamPageImpl<NodeContent> fetchFilteredNodes(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		NodeRoot nodeRoot = gc.getProject().getNodeRoot();

		List<String> languageTags = getLanguageArgument(env);
		ContainerType type = getNodeVersion(env);

		Stream<NodeContent> contents = nodeRoot.findAllStream(gc, READ_PUBLISHED_PERM)
			// Now lets try to load the containers for those found nodes - apply the language fallback
			.map(node -> new NodeContent(node, node.findVersion(gc, languageTags, type), languageTags, type))
			// Filter nodes without a container
			.filter(content -> content.getContainer() != null)
			.filter(gc::hasReadPerm);

		return applyNodeFilter(env, contents);
	}

	protected DynamicStreamPageImpl<NodeContent> applyNodeFilter(DataFetchingEnvironment env, Stream<? extends NodeContent> stream) {
		Map<String, ?> filterArgument = env.getArgument("filter");
		PagingParameters pagingInfo = getPagingInfo(env);
		GraphQLContext gc = env.getContext();

		if (filterArgument != null) {
			return new DynamicStreamPageImpl<>(stream, pagingInfo, NodeFilter.filter(gc).createPredicate(filterArgument));
		} else {
			return new DynamicStreamPageImpl<>(stream, pagingInfo);
		}
	}
}
