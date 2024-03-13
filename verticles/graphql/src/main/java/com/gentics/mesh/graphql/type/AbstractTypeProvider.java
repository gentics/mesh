package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.action.DAOActionContext.context;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static graphql.Scalars.GraphQLString;
import static graphql.scalars.java.JavaPrimitives.GraphQLLong;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLEnumType.newEnum;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.graphqlfilter.Sorting;
import com.gentics.graphqlfilter.filter.operation.Comparison;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.graphqlfilter.filter.operation.UnformalizableQuery;
import com.gentics.mesh.core.action.DAOActions;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingRootDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.PermissionException;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.NativeQueryFiltering;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.EntityFilter;
import com.gentics.mesh.graphql.filter.NodeFilter;
import com.gentics.mesh.graphql.model.NativeFilter;
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
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractTypeProvider {

	private static final Logger log = LoggerFactory.getLogger(AbstractTypeProvider.class);
	public static final String LINK_TYPE_NAME = "LinkType";
	public static final String NATIVE_FILTER_NAME = "NativeFilter";
	public static final String NODE_CONTAINER_VERSION_NAME = "NodeVersion";

	protected final MeshOptions options;

	public AbstractTypeProvider(MeshOptions options) {
		this.options = options;
	}

	public GraphQLEnumType createNativeFilterEnumType() {
		GraphQLEnumType nativeFilterEnum = newEnum().name(NATIVE_FILTER_NAME).description("Usage of native database-level filtering, instead of default provided by Mesh")
				.value(NativeFilter.IF_POSSIBLE.name(), NativeFilter.IF_POSSIBLE, "Try native filter first, fall back to Mesh otherwise. Does not apply native paging, if no filter is specified.")
				.value(NativeFilter.ONLY.name(), NativeFilter.ONLY, "Force native filters only. If no filtering but paging is specified, apply the native paging.")
				.value(NativeFilter.NEVER.name(), NativeFilter.NEVER, "Force default Mesh filtering and paging. Ignored, if sorting is specified.").build();
			return nativeFilterEnum;
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
	public List<GraphQLArgument> createPagingArgs(boolean deprecateSorting) {
		List<GraphQLArgument> arguments = new ArrayList<>();

		Function<GraphQLArgument.Builder, GraphQLArgument.Builder> applyDeprecation = b -> {
			if (deprecateSorting) {
				b.deprecate("Use 'sort' argument to set multiple sort targets");
			}
			return b;
		};

		// #page
		arguments.add(newArgument().name("page").defaultValue(1L).description("Page to be selected").type(GraphQLLong).build());

		// #perPage
		arguments.add(newArgument().name("perPage").description("Max count of elements per page").type(GraphQLLong).build());

		// #sortBy
		arguments.add(applyDeprecation.apply(newArgument()).name("sortBy").description("Field to sort the elements by").type(GraphQLString).build());

		// #sortOrder
		arguments.add(applyDeprecation.apply(newArgument()).name("sortOrder").type(new GraphQLTypeReference(Sorting.SORT_ORDER_NAME)).description("Order to sort the elements in").build());

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
	public List<String> getLanguageArgument(DataFetchingEnvironment env, HibFieldContainer source) {
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

	public GraphQLArgument createNativeFilterArg() {
		return newArgument().name("nativeFilter").type(new GraphQLTypeReference(NATIVE_FILTER_NAME)).defaultValue(NativeFilter.IF_POSSIBLE).description(
			"Specify whether the native database-level filtering should be used.").build();
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
	protected HibCoreElement<?> handleUuidNameArgs(DataFetchingEnvironment env, Object parent, DAOActions<?, ?> actions) {
		GraphQLContext gc = env.getContext();
		HibCoreElement<?> element = handleUuidNameArgsNoPerm(env, uuid -> actions.loadByUuid(context(Tx.get(), gc, parent), uuid, null, false),
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
	protected HibCoreElement<?> handleUuidNameArgsNoPerm(DataFetchingEnvironment env, Function<String, HibCoreElement<?>> uuidFetcher,
		Function<String, HibCoreElement<?>> nameFetcher) {
		String uuid = env.getArgument("uuid");
		HibCoreElement<?> element = null;
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

	protected HibBaseElement handleBranchSchema(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		HibBranch branch = env.getSource();
		Stream<? extends HibSchemaVersion> schemas = StreamSupport.stream(branch.findActiveSchemaVersions().spliterator(), false);
		UserDao userDao = Tx.get().userDao();

		// We need to handle permissions here since we check the schema container perm and not the schema container version perm.
		return handleUuidNameArgsNoPerm(env, uuid -> schemas.filter(schema -> {
			HibSchema container = schema.getSchemaContainer();
			return container.getUuid().equals(uuid) && userDao.hasPermission(gc.getUser(), container, READ_PERM);
		}).findFirst().get(), name -> schemas.filter(schema -> schema.getName().equals(name) && userDao.hasPermission(gc.getUser(), schema
			.getSchemaContainer(), READ_PERM)).findFirst().get());
	}

	protected Page<HibSchemaVersion> handleBranchSchemas(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		HibBranch branch = env.getSource();
		UserDao userDao = Tx.get().userDao();

		Stream<? extends HibSchemaVersion> schemas = StreamSupport.stream(branch.findActiveSchemaVersions().spliterator(), false).filter(
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
	protected <T extends HibCoreElement<?>> GraphQLFieldDefinition newPagingSearchField(String name, String description,
		DAOActions<T, ?> actions,
		String pageTypeName, SearchHandler<?, ?> searchHandler, EntityFilter<T> filterProvider) {
		Builder fieldDefBuilder = newFieldDefinition()
			.name(name)
			.description(description)
			.argument(createPagingArgs(filterProvider != null))
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
						Pair<Predicate<T>, Optional<FilterOperation<?>>> filters = parseFilters(env, filterProvider);
						return filters.getRight().map(filter1 -> actions.loadAll(context(Tx.get(), gc, env.getSource()), getPagingInfo(env), filter1))
								.orElse((Page) actions.loadAll(context(Tx.get(), gc, env.getSource()), getPagingInfo(env), filters.getLeft()));
					} else {
						return actions.loadAll(context(Tx.get(), gc, env.getSource()), getPagingInfo(env));
					}
				}
			});

		if (filterProvider != null) {
			fieldDefBuilder
				.argument(createNativeFilterArg())
				.argument(filterProvider.createFilterArgument())
				.argument(filterProvider.createSortArgument());
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
		return newPagingFieldWithFetcherBuilder(name, description, dataFetcher, referenceTypeName, false).build();
	}

	protected graphql.schema.GraphQLFieldDefinition.Builder newPagingFieldWithFetcherBuilder(String name, String description,
		DataFetcher<?> dataFetcher, String pageTypeName, boolean deprecateSorting) {
		return newFieldDefinition().name(name).description(description)
			.argument(createPagingArgs(deprecateSorting))
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
		PagingParametersImpl parameters = new PagingParametersImpl();
		Long page = env.getArgument("page");
		if (page != null) {
			parameters.setPage(page);
		}
		Long perPage = env.getArgument("perPage");
		if (perPage != null) {
			parameters.setPerPage(perPage);
		}
		String sortBy = env.getArgument("sortBy");
		SortOrder sortOrder = env.getArgument("sortOrder");
		if (StringUtils.isNotBlank(sortBy) && sortOrder != null) {
			parameters.putSort(sortBy, sortOrder);
		}
		Map<String, ?> sortArgument = env.getArgument("sort");
		parameters.putSort(parseGraphQlSort(sortArgument, Optional.empty()));
		parameters.validate();
		return parameters;
	}

	@SuppressWarnings("unchecked")
	private static final Map<String, SortOrder> parseGraphQlSort(Map<String, ?> sort, Optional<String> prefix) {
		if (sort == null) {
			return Collections.emptyMap();
		}
		return sort.entrySet().stream().flatMap(entry -> sortOrderFromGraphQlSorting(entry.getValue())
				.map(order -> Stream.of(Pair.of(prefix.map(p -> p + "." + entry.getKey()).orElse(entry.getKey()), order)))
				.orElseGet(() -> {
					if (!Map.class.isInstance(entry.getValue())) {
						throw new IllegalArgumentException("Unexpected sort value for key '" + entry.getKey() + "':"  + entry.getValue());
					}
					return parseGraphQlSort((Map<String, Object>) entry.getValue(), Optional.ofNullable(prefix.map(p -> p + "." + entry.getKey()).orElse(entry.getKey())))
							.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue()));
				})
			).collect(Collectors.toMap(e -> e.getLeft(), e -> e.getRight(), (a, b) -> a)) ;
	}

	/**
	 * Fetches nodes and applies filters, either database-native or java.
	 *
	 * @param env
	 *            the environment of the request
	 * @return the filtered nodes
	 */
	protected DynamicStreamPageImpl<NodeContent> fetchFilteredNodes(DataFetchingEnvironment env) {
		Tx tx = Tx.get();
		NodeDao nodeDao = tx.nodeDao();
		GraphQLContext gc = env.getContext();
		HibProject project = tx.getProject(gc);

		List<String> languageTags = getLanguageArgument(env);
		ContainerType type = getNodeVersion(env);

		NodeFilter nodeFilter = NodeFilter.filter(gc);
		Pair<Predicate<NodeContent>, Optional<FilterOperation<?>>> filters = parseFilters(env, nodeFilter);

		PagingParameters pagingInfo = getPagingInfo(env);
		return applyNodeFilter(filters.getRight().isPresent() || PersistingRootDao.shouldSort(pagingInfo)
				? nodeDao.findAllContent(project, gc, languageTags, type, pagingInfo, filters.getRight()) 
				: nodeDao.findAllContent(project, gc, languageTags, type), 
			pagingInfo, filters.getLeft(), filters.getRight().isPresent());
	}

	public <T> Pair<Predicate<T>,Optional<FilterOperation<?>>> parseFilters(DataFetchingEnvironment env, EntityFilter<T> filterProvider) {
		GraphQLContext gc = env.getContext();
		Map<String, ?> filterArgument = env.getArgument("filter");
		NativeQueryFiltering nativeQueryFiltering = options.getNativeQueryFiltering();
		Predicate<T> javaFilter = null;
		Optional<FilterOperation<?>> maybeNativeFilter = Optional.empty();

		NativeFilter envNativeFilter = env.getArgumentOrDefault("nativeFilter", NativeFilter.IF_POSSIBLE);
		if (filterArgument != null) {
			switch (nativeQueryFiltering) {
			case ON_DEMAND:
				if (NativeFilter.NEVER.equals(envNativeFilter)) {
					javaFilter = filterProvider.createPredicate(filterArgument);
					break;
				}
				boolean invalid = true;
				if (NativeFilter.ONLY.equals(envNativeFilter) || NativeFilter.IF_POSSIBLE.equals(envNativeFilter)) {
					invalid = false;
				}
				if (invalid) {
					throw new InvalidParameterException("Unsupported 'nativeFilter' parameter value: " + envNativeFilter);
				}
				// else fall through into the native filtering
			case ALWAYS:
				if (NativeQueryFiltering.ALWAYS.equals(nativeQueryFiltering) && NativeFilter.NEVER.equals(envNativeFilter)) {
					throw new InvalidParameterException("Conflicting params: requested GraphQL 'nativeFilter' = " + envNativeFilter + ", Mesh GraphQL Options 'nativeQueryFiltering' = " + nativeQueryFiltering);
				}
				try {
					maybeNativeFilter = Optional.of(filterProvider.createFilterOperation(filterArgument));
					break;
				} catch (UnformalizableQuery e) {
					log.warn("The query filter cannot be formalized: {}", filterArgument);
					log.debug(e);
					if (NativeQueryFiltering.ALWAYS.equals(nativeQueryFiltering) || NativeFilter.ONLY.equals(envNativeFilter)) {
						throw new InvalidParameterException("Cannot proceed with an unformalizable query on params: requested GraphQL 'nativeFilter' = " + envNativeFilter 
								+ ", Mesh GraphQL Options 'nativeQueryFiltering' = " + nativeQueryFiltering + ", failed filter = " + e.getFilter());
					} else {
						log.info("Trying to apply old Java filtering");
					}// fall through into the old filtering
				}			
			case NEVER:
				if (NativeFilter.ONLY.equals(envNativeFilter)) {
					throw new InvalidParameterException("Conflicting params: requested GraphQL 'nativeFilter' = " + envNativeFilter + ", Mesh GraphQL Options 'nativeQueryFiltering' = " + nativeQueryFiltering);
				}
				javaFilter = filterProvider.createPredicate(filterArgument);
				break;
			}
		} else {
			if (nativeQueryFiltering == NativeQueryFiltering.ALWAYS || envNativeFilter == NativeFilter.ONLY || PersistingRootDao.shouldSort(getPagingInfo(env))) {
				// Force native filtering with `1 = 1` dummy filter
				maybeNativeFilter = Optional.of(Comparison.dummy(true, StringUtils.EMPTY));
				if (nativeQueryFiltering == NativeQueryFiltering.NEVER) {
					log.warn("A sorting is requested with native query filtering turned off. This may result in performance penalties!");
				}
			}
		}
		return Pair.of(javaFilter, maybeNativeFilter);
	}

	/**
	 * Apply Java filtering to a stream.
	 * 
	 * @param stream
	 * @param pagingInfo
	 * @param javaFilter
	 * @param ignorePaging
	 * @return
	 */
	protected DynamicStreamPageImpl<NodeContent> applyNodeFilter(Stream<? extends NodeContent> stream, PagingParameters pagingInfo, Predicate<NodeContent> javaFilter, boolean ignorePaging) {
		return new DynamicStreamPageImpl<>(stream, pagingInfo, javaFilter, ignorePaging);
	}

	/**
	 * Apply GraphQL filter to a stream as Java filter.
	 * 
	 * @param env
	 * @param stream
	 * @param ignorePaging
	 * @return
	 */
	protected DynamicStreamPageImpl<NodeContent> applyNodeFilter(DataFetchingEnvironment env, Stream<? extends NodeContent> stream, boolean ignorePaging, boolean ignoreFiltering) {
		Map<String, ?> filterArgument = ignoreFiltering ? null : env.getArgument("filter");
		GraphQLContext gc = env.getContext();
		Predicate<NodeContent> predicate = null;

		if (filterArgument != null) {
			predicate = NodeFilter.filter(gc).createPredicate(filterArgument);
		}
		return applyNodeFilter(stream, getPagingInfo(env), predicate, ignorePaging);
	}

	/**
	 * Map GraphQL {@link Sorting} value onto a Mesh {@link SortOrder}.
	 * 
	 * @param sorting
	 * @return
	 */
	private static final Optional<SortOrder> sortOrderFromGraphQlSorting(Object sorting) {
		if (sorting != null && sorting instanceof Sorting) {
			Sorting s = Sorting.class.cast(sorting);
			switch (s) {
			case ASCENDING:
				return Optional.of(SortOrder.ASCENDING);
			case DESCENDING:
				return Optional.of(SortOrder.DESCENDING);
			}
			throw new IllegalStateException("Impossible case of unsupported Sorting value " + sorting);
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Create a dummy field for the schema type definition.
	 * 
	 * @param description
	 * @return
	 */
	protected static final FieldSchema emptySchemaFieldDummy(String description) {
		return new FieldSchema() {

			@Override
			public void validate() {
}

			@Override
			public FieldSchema setRequired(boolean isRequired) {
				return this;
			}

			@Override
			public FieldSchema setName(String name) {
				return this;
			}

			@Override
			public FieldSchema setLabel(String label) {
				return this;
			}

			@Override
			public FieldSchema setElasticsearch(JsonObject elasticsearch) {
				return this;
			}

			@Override
			public boolean isRequired() {
				return false;
			}

			@Override
			public String getType() {
				return StringUtils.EMPTY;
			}

			@Override
			public String getName() {
				return "EMPTY";
			}

			@Override
			public String getLabel() {
				return description;
			}

			@Override
			public JsonObject getElasticsearch() {
				return null;
			}

			@Override
			public Map<String, Object> getAllChangeProperties() {
				return null;
			}

			@Override
			public SchemaChangeModel compareTo(FieldSchema fieldSchema) {
				return null;
			}

			@Override
			public void apply(Map<String, Object> fieldProperties) {
			}

			@Override
			public boolean isNoIndex() {
				return false;
			}

			@Override
			public FieldSchema setNoIndex(boolean isNoIndex) {
				return null;
			}
		};
	}
}
