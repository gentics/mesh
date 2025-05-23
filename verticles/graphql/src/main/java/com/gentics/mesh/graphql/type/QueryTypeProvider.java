package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.graphql.type.BranchTypeProvider.BRANCH_TYPE_NAME;
import static com.gentics.mesh.graphql.type.GroupTypeProvider.GROUP_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.GroupTypeProvider.GROUP_TYPE_NAME;
import static com.gentics.mesh.graphql.type.MicroschemaTypeProvider.MICROSCHEMA_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.MicroschemaTypeProvider.MICROSCHEMA_TYPE_NAME;
import static com.gentics.mesh.graphql.type.NodeReferenceTypeProvider.NODE_REFERENCE_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.NodeReferenceTypeProvider.NODE_REFERENCE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.NodeTypeProvider.createNodeContentWithSoftPermissions;
import static com.gentics.mesh.graphql.type.PluginTypeProvider.PLUGIN_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.PluginTypeProvider.PLUGIN_TYPE_NAME;
import static com.gentics.mesh.graphql.type.ProjectReferenceTypeProvider.PROJECT_REFERENCE_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.ProjectReferenceTypeProvider.PROJECT_REFERENCE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.ProjectTypeProvider.PROJECT_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.ProjectTypeProvider.PROJECT_TYPE_NAME;
import static com.gentics.mesh.graphql.type.RoleTypeProvider.ROLE_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.RoleTypeProvider.ROLE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.SchemaTypeProvider.SCHEMA_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.SchemaTypeProvider.SCHEMA_TYPE_NAME;
import static com.gentics.mesh.graphql.type.TagFamilyTypeProvider.TAG_FAMILY_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.TagFamilyTypeProvider.TAG_FAMILY_TYPE_NAME;
import static com.gentics.mesh.graphql.type.TagTypeProvider.TAG_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.TagTypeProvider.TAG_TYPE_NAME;
import static com.gentics.mesh.graphql.type.UserTypeProvider.USER_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.UserTypeProvider.USER_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.scalars.java.JavaPrimitives.GraphQLLong;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.graphqlfilter.Sorting;
import com.gentics.mesh.cache.GraphQLSchemaCache;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.action.DAOActionsCollection;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.PermissionException;
import com.gentics.mesh.core.rest.error.UuidNotFoundException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.GroupFilter;
import com.gentics.mesh.graphql.filter.MicronodeFilter;
import com.gentics.mesh.graphql.filter.NodeFilter;
import com.gentics.mesh.graphql.filter.NodeReferenceFilter;
import com.gentics.mesh.graphql.filter.RoleFilter;
import com.gentics.mesh.graphql.filter.UserFilter;
import com.gentics.mesh.graphql.type.field.FieldDefinitionProvider;
import com.gentics.mesh.graphql.type.field.MicronodeFieldTypeProvider;
import com.gentics.mesh.handler.Versioned;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.search.index.group.GroupSearchHandler;
import com.gentics.mesh.search.index.project.ProjectSearchHandler;
import com.gentics.mesh.search.index.role.RoleSearchHandler;
import com.gentics.mesh.search.index.tag.TagSearchHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilySearchHandler;
import com.gentics.mesh.search.index.user.UserSearchHandler;

import graphql.ExceptionWhileDataFetching;
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;

/**
 * The {@link QueryTypeProvider} provides as the name suggests the query type for the GraphQL schema. This type is the starting point for all GraphQL queries.
 * Various other schema types are located in dedicated classes for each type. Dependency injection is used to load those dependencies and thus make these types
 * accessible by the root type. Please note that this root type is and will most likely always be project specific. It is not possible to query other projects.
 * Only the currently selected project and global elements (user, roles, groups..) can be queries.
 * 
 * We must enforce this limitation because GraphQL schemas can't handle dynamic types. It is not possible to distinguish between a content schema v1 or projectA
 * and content schema v2 of projectB. It is not possible to access data across branches due to the same reason. A different branch may use different schema
 * versions.
 */
@Singleton
public class QueryTypeProvider extends AbstractTypeProvider {

	protected final MeshTypeProvider meshTypeProvider;

	protected final InterfaceTypeProvider interfaceTypeProvider;

	protected final MicronodeFieldTypeProvider micronodeFieldTypeProvider;

	protected final FieldDefinitionProvider fieldDefProvider;

	protected final NodeTypeProvider nodeTypeProvider;

	protected final NodeReferenceTypeProvider nodeReferenceTypeProvider;

	protected final ProjectTypeProvider projectTypeProvider;

	protected final ProjectReferenceTypeProvider projectReferenceTypeProvider;

	protected final UserTypeProvider userTypeProvider;

	protected final TagTypeProvider tagTypeProvider;

	protected final TagFamilyTypeProvider tagFamilyTypeProvider;

	protected final RoleTypeProvider roleTypeProvider;

	protected final GroupTypeProvider groupTypeProvider;

	protected final WebRootService webrootService;

	protected final BootstrapInitializer boot;

	protected final BranchTypeProvider branchTypeProvider;

	protected final SchemaTypeProvider schemaTypeProvider;

	protected final MicroschemaTypeProvider microschemaTypeProvider;

	protected final UserSearchHandler userSearchHandler;

	protected final RoleSearchHandler roleSearchHandler;

	protected final GroupSearchHandler groupSearchHandler;

	protected final ProjectSearchHandler projectSearchHandler;

	protected final TagFamilySearchHandler tagFamilySearchHandler;

	protected final TagSearchHandler tagSearchHandler;

	protected final PluginTypeProvider pluginProvider;

	protected final PluginApiTypeProvider pluginApiProvider;

	protected final DAOActionsCollection actions;

	protected final GraphQLSchemaCache cache;

	@Inject
	public QueryTypeProvider(MeshOptions options, MeshTypeProvider meshTypeProvider,
			InterfaceTypeProvider interfaceTypeProvider, MicronodeFieldTypeProvider micronodeFieldTypeProvider,
			FieldDefinitionProvider fieldDefProvider, NodeTypeProvider nodeTypeProvider,
			NodeReferenceTypeProvider nodeReferenceTypeProvider, ProjectTypeProvider projectTypeProvider,
			ProjectReferenceTypeProvider projectReferenceTypeProvider, UserTypeProvider userTypeProvider,
			TagTypeProvider tagTypeProvider, TagFamilyTypeProvider tagFamilyTypeProvider,
			RoleTypeProvider roleTypeProvider, GroupTypeProvider groupTypeProvider, WebRootService webrootService,
			BootstrapInitializer boot, BranchTypeProvider branchTypeProvider, SchemaTypeProvider schemaTypeProvider,
			MicroschemaTypeProvider microschemaTypeProvider, UserSearchHandler userSearchHandler,
			RoleSearchHandler roleSearchHandler, GroupSearchHandler groupSearchHandler,
			ProjectSearchHandler projectSearchHandler, TagFamilySearchHandler tagFamilySearchHandler,
			TagSearchHandler tagSearchHandler, PluginTypeProvider pluginProvider,
			PluginApiTypeProvider pluginApiProvider, DAOActionsCollection actions, GraphQLSchemaCache cache) {
		super(options);
		this.meshTypeProvider = meshTypeProvider;
		this.interfaceTypeProvider = interfaceTypeProvider;
		this.micronodeFieldTypeProvider = micronodeFieldTypeProvider;
		this.fieldDefProvider = fieldDefProvider;
		this.nodeTypeProvider = nodeTypeProvider;
		this.nodeReferenceTypeProvider = nodeReferenceTypeProvider;
		this.projectTypeProvider = projectTypeProvider;
		this.projectReferenceTypeProvider = projectReferenceTypeProvider;
		this.userTypeProvider = userTypeProvider;
		this.tagTypeProvider = tagTypeProvider;
		this.tagFamilyTypeProvider = tagFamilyTypeProvider;
		this.roleTypeProvider = roleTypeProvider;
		this.groupTypeProvider = groupTypeProvider;
		this.webrootService = webrootService;
		this.boot = boot;
		this.branchTypeProvider = branchTypeProvider;
		this.schemaTypeProvider = schemaTypeProvider;
		this.microschemaTypeProvider = microschemaTypeProvider;
		this.userSearchHandler = userSearchHandler;
		this.roleSearchHandler = roleSearchHandler;
		this.groupSearchHandler = groupSearchHandler;
		this.projectSearchHandler = projectSearchHandler;
		this.tagFamilySearchHandler = tagFamilySearchHandler;
		this.tagSearchHandler = tagSearchHandler;
		this.pluginProvider = pluginProvider;
		this.pluginApiProvider = pluginApiProvider;
		this.actions = actions;
		this.cache = cache;
	}

	/**
	 * Get the field definition provider
	 * @return field definition provider
	 */
	public FieldDefinitionProvider getFieldDefProvider() {
		return fieldDefProvider;
	}

	/**
	 * Fetch multiple nodes via UUID.
	 *
	 * <p>
	 * When there is no node for a given UUID or the user does not have the necessary permissions, the respective errors will be added to the execution context.
	 * </p>
	 *
	 * <p>
	 * The resulting items will be filtered by {@link AbstractTypeProvider#applyNodeFilter(DataFetchingEnvironment, Stream) applyNodeFilter()}.
	 * </p>
	 *
	 * @param env
	 * @return A page containing all found nodes matching the given UUIDs
	 */
	private DataFetcherResult<Page<NodeContent>> fetchNodesByUuid(DataFetchingEnvironment env) {
		Tx tx = Tx.get();
		ContentDao contentDao = tx.contentDao();
		NodeDao nodeDao = tx.nodeDao();

		List<String> uuids = env.getArgument("uuids");

		if (uuids == null || uuids.isEmpty()) {
			return DataFetcherResult.<Page<NodeContent>>newResult()
					.data(new DynamicStreamPageImpl<>(Stream.empty(), getPagingInfo(env)))
					.errors(Collections.emptyList()).build();
		}

		GraphQLContext gc = env.getContext();
		HibProject project = tx.getProject(gc);
		List<String> languageTags = getLanguageArgument(env);
		ContainerType type = getNodeVersion(env);
		List<GraphQLError> errors = new ArrayList<>();
		List<NodeContent> contents = uuids.stream()
			// When a node cannot be found, we still need the UUID for the error message.
			.map(uuid -> Pair.of(uuid, nodeDao.findByUuid(project, uuid)))
			.map(node -> {
				Throwable error = null;

				if (node.getRight() == null) {
					error = new UuidNotFoundException("node", node.getLeft());
				} else {
					// The node was found, check the permissions.
					try {
						return (HibNode) gc.requiresPerm(node.getRight(), READ_PERM, READ_PUBLISHED_PERM);
					} catch (PermissionException e) {
						error = e;
					}
				}

				errors.add(new ExceptionWhileDataFetching(env.getExecutionStepInfo().getPath(), error, env.getField().getSourceLocation()));

				return null;
			})
			.filter(Objects::nonNull)
			.map(node -> {
				HibNodeFieldContainer container = contentDao.findVersion(node, gc, languageTags, type);
				return new NodeContent(node, container, languageTags, type);
			})
			.filter(content -> content.getContainer() != null)
			.filter(content1 -> gc.hasReadPerm(content1, type)).collect(Collectors.toList());

		return DataFetcherResult.<Page<NodeContent>>newResult().data(applyNodeFilter(env, contents.stream(), false, false, Optional.empty())).errors(errors).build();
	}

	/**
	 * Data fetcher for nodes.
	 *
	 * @param env
	 * @return
	 */
	public Object nodeFetcher(DataFetchingEnvironment env) {
		Tx tx = Tx.get();
		ContentDao contentDao = tx.contentDao();
		String uuid = env.getArgument("uuid");
		if (uuid != null) {
			NodeDao nodeDao = tx.nodeDao();
			GraphQLContext gc = env.getContext();
			HibNode node = nodeDao.findByUuid(tx.getProject(gc), uuid);
			if (node == null) {
				// TODO Throw graphql aware not found exception
				return null;
			}
			List<String> languageTags = getLanguageArgument(env);
			ContainerType type = getNodeVersion(env);

			node = gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
			HibNodeFieldContainer container = contentDao.findVersion(node, gc, languageTags, type);

			return createNodeContentWithSoftPermissions(env, gc, node, languageTags, type, container);
		}
		String path = env.getArgument("path");
		if (path != null) {
			GraphQLContext gc = env.getContext();
			ContainerType type = getNodeVersion(env);

			Path pathResult = webrootService.findByProjectPath(gc, path, type);

			if (pathResult.getLast() == null || !pathResult.isFullyResolved()) {
				return null;
			}

			// TODO HIB
			PathSegment graphSegment = pathResult.getLast();
			HibNodeFieldContainer container = graphSegment.getContainer();
			HibNode nodeOfContainer = contentDao.getNode(container);

			nodeOfContainer = gc.requiresPerm(nodeOfContainer, READ_PERM, READ_PUBLISHED_PERM);
			List<String> langs = new ArrayList<>();
			if (container != null) {
				langs.add(container.getLanguageTag());
			}
			return createNodeContentWithSoftPermissions(env, gc, nodeOfContainer, langs, type, container);
		}
		return null;
	}

	/**
	 * Data fetcher for the currently active user.
	 * 
	 * @param env
	 * @return
	 */
	public Object userMeFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		HibUser requestUser = gc.getUser();
		// No need to check for permissions. The user should always be able to read himself
		return requestUser;
	}

	/**
	 * Data fetcher for the current project.
	 * 
	 * @param env
	 * @return
	 */
	public Object projectFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		HibProject project = Tx.get().getProject(gc);
		return gc.requiresPerm(project, READ_PERM);
	}

	/**
	 * Data fetcher for the current branch.
	 * 
	 * @param env
	 * @return
	 */
	public Object branchFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		HibBranch branch = Tx.get().getBranch(gc);
		return gc.requiresPerm(branch, READ_PERM);
	}

	/**
	 * Data fetcher for the root node of the current project.
	 * 
	 * @param env
	 * @return
	 */
	public Object rootNodeFetcher(DataFetchingEnvironment env) {
		Tx tx = Tx.get();
		ContentDao contentDao = tx.contentDao();
		GraphQLContext gc = env.getContext();
		HibProject project = tx.getProject(gc);
		if (project != null) {
			HibNode node = project.getBaseNode();
			ContainerType type = getNodeVersion(env);
			gc.requiresPerm(node, READ_PUBLISHED_PERM, READ_PERM);
			List<String> languageTags = getLanguageArgument(env);
			HibNodeFieldContainer container = contentDao.findVersion(node, gc, languageTags, type);

			return createNodeContentWithSoftPermissions(env, gc, node, languageTags, type, container);
		}
		return null;
	}

	/**
	 * Construct the query/root type for the current project.
	 * 
	 * @param context
	 * @return
	 */
	public GraphQLObjectType getRootType(GraphQLContext context) {
		Builder root = newObject();
		root.name("Query");

		// .me
		root.field(newFieldDefinition().name("me")
			.description("The current user")
			.type(new GraphQLTypeReference(USER_TYPE_NAME))
			.dataFetcher(this::userMeFetcher).build());

		// .project
		root.field(newFieldDefinition().name("project")
			.description("Load the project that is active for this GraphQL query.")
			.type(new GraphQLTypeReference(PROJECT_TYPE_NAME)).dataFetcher(this::projectFetcher).build());

		// NOT ALLOWED - See class description for details
		// .projects
		// root.field(newPagingField("projects", "Load page of projects.", (ac) -> boot.projectRoot(), "Project"));

		// .node
		root.field(newFieldDefinition().name("node")
			.description("Load a node by uuid, uuid and language or webroot path.")
			.argument(createUuidArg("Node uuid"))
			.argument(createLanguageTagArg(true))
			.argument(createPathArg())
			.argument(createNodeVersionArg())
			.dataFetcher(this::nodeFetcher)
			.type(new GraphQLTypeReference(NODE_TYPE_NAME)).build());

		// .nodes
		NodeFilter nodeFilter = NodeFilter.filter(context);
		root.field(newFieldDefinition().name("nodes")
			.description("Load a page of nodes via the regular nodes list or via a search.")
			.argument(createPagingArgs(true))
			.argument(createQueryArg())
			.argument(createUuidsArg("Node uuids"))
			.argument(createLanguageTagArg(true))
			.argument(createNodeVersionArg())
			.argument(nodeFilter.createFilterArgument())
			.argument(nodeFilter.createSortArgument())
			.argument(createNativeFilterArg())
			.type(new GraphQLTypeReference(NODE_PAGE_TYPE_NAME))
			.dataFetcher(env -> {
				String query = env.getArgument("query");

				// Check whether we need to load the nodes via a query or regular project-wide paging
				if (query != null) {
					GraphQLContext gc = env.getContext();
					// TODO add filtering for query nodes
					ContainerType type = getNodeVersion(env);
					gc.getNodeParameters().setLanguages(getLanguageArgument(env).stream().toArray(String[]::new));
					return nodeTypeProvider.handleContentSearch(gc, query, getPagingInfo(env), type);
				}

				if (env.containsArgument("uuids")) {
					return fetchNodesByUuid(env);
				}

				return fetchFilteredNodes(env);
			}));

		// .rootNode
		root.field(newFieldDefinition()
			.name("rootNode")
			.description("Return the project root node.")
			.argument(createLanguageTagArg(true))
			.argument(createNodeVersionArg())
			.type(new GraphQLTypeReference(NODE_TYPE_NAME))
			.dataFetcher(this::rootNodeFetcher).build());

		// .tag
		// TODO use project specific tag root
		root.field(newElementField("tag", "Load first tag by name or uuid.", actions.tagActions(), TAG_TYPE_NAME));

		// .tags
		root.field(newPagingSearchField("tags", "Load page of tags.", actions.tagActions(), TAG_PAGE_TYPE_NAME, tagSearchHandler, null));

		// .tagFamily
		root.field(newElementField("tagFamily", "Load tagFamily by name or uuid.", actions.tagFamilyActions(), TAG_FAMILY_TYPE_NAME));

		// .tagFamilies
		// TODO fix me. The root of the project should be used and not the global one
		root.field(newPagingSearchField("tagFamilies", "Load page of tagFamilies.", actions.tagFamilyActions(), TAG_FAMILY_PAGE_TYPE_NAME,
			tagFamilySearchHandler, null));

		// .branch
		root.field(newFieldDefinition().name("branch").description("Load the branch that is active for this GraphQL query.")
			.type(new GraphQLTypeReference(BRANCH_TYPE_NAME)).dataFetcher(this::branchFetcher).build());

		// .schema
		root.field(newElementField("schema", "Load schema by name or uuid.", actions.schemaActions(), SCHEMA_TYPE_NAME));

		// .schemas
		root.field(newPagingField("schemas", "Load page of schemas.", actions.schemaActions(), SCHEMA_PAGE_TYPE_NAME));

		// .microschema
		root.field(
			newElementField("microschema", "Load microschema by name or uuid.", actions.microschemaActions(), MICROSCHEMA_TYPE_NAME));

		// .microschemas
		root.field(newPagingField("microschemas", "Load page of microschemas.", actions.microschemaActions(), MICROSCHEMA_PAGE_TYPE_NAME));

		// .role
		root.field(newElementField("role", "Load role by name or uuid.", actions.roleActions(), ROLE_TYPE_NAME));

		// .roles
		root.field(newPagingSearchField("roles", "Load page of roles.", actions.roleActions(), ROLE_PAGE_TYPE_NAME, roleSearchHandler,
			RoleFilter.filter()));

		// .group
		root.field(newElementField("group", "Load group by name or uuid.", actions.groupActions(), GROUP_TYPE_NAME));

		// .groups
		root.field(newPagingSearchField("groups", "Load page of groups.", actions.groupActions(), GROUP_PAGE_TYPE_NAME, groupSearchHandler,
			GroupFilter.filter()));

		// .user
		root.field(newElementField("user", "Load user by name or uuid.", actions.userActions(), USER_TYPE_NAME, true));

		// .users
		root.field(newPagingSearchField("users", "Load page of users.", actions.userActions(), USER_PAGE_TYPE_NAME, userSearchHandler,
			UserFilter.filter()));

		// .plugin
		root.field(pluginProvider.createPluginField());

		// .plugins
		root.field(pluginProvider.createPluginPageField());

		// .pluginApi
		if (pluginApiProvider.hasPlugins()) {
			root.field(pluginApiProvider.createPluginAPIField());
		}

		// .mesh
		root.field(meshTypeProvider.createMeshFieldType());

		return root.build();
	}

	/**
	 * Construct a page type with the given element type for its nested elements.
	 * 
	 * @param pageTypeName
	 *            Name of the element that is being nested
	 * @param elementType
	 *            Type of the nested element
	 * @return
	 */
	private GraphQLObjectType newPageType(String pageTypeName, String elementType) {

		Builder type = newObject().name(pageTypeName).description("Paged result");
		type.field(newFieldDefinition().name("elements").type(new GraphQLList(new GraphQLTypeReference(elementType))).dataFetcher(env -> {
			return env.getSource();
		}));

		type.field(newFieldDefinition().name("totalCount").description("Return the total item count which the resource could provide.")
			.dataFetcher(env -> {
				Page<?> page = env.getSource();
				return page.getTotalElements();
			}).type(GraphQLLong));

		type.field(newFieldDefinition().name("currentPage").description("Return the current page number.").dataFetcher(env -> {
			Page<?> page = env.getSource();
			return page.getNumber();
		}).type(GraphQLLong));

		type.field(newFieldDefinition().name("pageCount").description("Return the total amount of pages which the resource can provide.")
			.dataFetcher(env -> {
				Page<?> page = env.getSource();
				return page.getPageCount();
			}).type(GraphQLLong));

		type.field(newFieldDefinition().name("perPage").description("Return the per page parameter value that was used to load the page.")
			.dataFetcher(env -> {
				Page<?> page = env.getSource();
				return page.getPerPage();
			}).type(GraphQLLong));

		type.field(newFieldDefinition().name("size").description(
			"Return the amount of items which the page is containing. Please note that a page may always contain less items compared to its maximum capacity.")
			.dataFetcher(env -> {
				Page<?> page = env.getSource();
				return page.getSize();
			}).type(GraphQLLong));

		type.field(newFieldDefinition().name("hasNextPage").description("Check whether the paged resource could serve another page")
			.type(GraphQLBoolean).dataFetcher(env -> {
				Page<?> page = env.getSource();
				return page.hasNextPage();
			}));

		type.field(newFieldDefinition().name("hasPreviousPage").description("Check whether the current page has a previous page.")
			.type(GraphQLBoolean).dataFetcher(env -> {
				Page<?> page = env.getSource();
				return page.hasPreviousPage();
			}));

		return type.build();
	}

	/**
	 * Construct the root schema.
	 * 
	 * @param context
	 * @return
	 */
	public GraphQLSchema getRootSchema(GraphQLContext context) {
		CommonTx tx = CommonTx.get();
		String cacheKey = getCacheKey(context);

		return cache.get(cacheKey, key -> {
			HibProject project = Tx.get().getProject(context);
			graphql.schema.GraphQLSchema.Builder builder = GraphQLSchema.newSchema();
			Set<GraphQLType> additionalTypes = new HashSet<>();

			additionalTypes.add(UserFilter.filter().createType());
			additionalTypes.add(UserFilter.filter().createSortingType());
			
			additionalTypes.add(NodeFilter.filter(context).createType());
			additionalTypes.add(NodeFilter.filter(context).createSortingType());
			
			for (byte features = 1; features <= NodeReferenceFilter.createLookupChange(true, true, true, true); features++) {
				additionalTypes.add(NodeReferenceFilter.nodeReferenceFilter(context, features).createType());
				additionalTypes.add(NodeReferenceFilter.nodeReferenceFilter(context, features).createSortingType());
			}
			
			additionalTypes.add(MicronodeFilter.filter(context).createType());
			additionalTypes.add(MicronodeFilter.filter(context).createSortingType());
			
			additionalTypes.add(schemaTypeProvider.createType(context));
			additionalTypes.add(newPageType(SCHEMA_PAGE_TYPE_NAME, SCHEMA_TYPE_NAME));

			additionalTypes.add(microschemaTypeProvider.createType());
			additionalTypes.add(newPageType(MICROSCHEMA_PAGE_TYPE_NAME, MICROSCHEMA_TYPE_NAME));

			additionalTypes.add(nodeTypeProvider.createVersionInfoType());
			additionalTypes.add(nodeTypeProvider.createType(context).forVersion(context));
			additionalTypes.add(newPageType(NODE_PAGE_TYPE_NAME, NODE_TYPE_NAME));

			micronodeFieldTypeProvider.createType(context).forVersion(context).ifPresent(additionalTypes::add);

			additionalTypes.add(nodeReferenceTypeProvider.createType());
			additionalTypes.add(newPageType(NODE_REFERENCE_PAGE_TYPE_NAME, NODE_REFERENCE_TYPE_NAME));

			additionalTypes.add(projectTypeProvider.createType(project));
			additionalTypes.add(newPageType(PROJECT_PAGE_TYPE_NAME, PROJECT_TYPE_NAME));

			additionalTypes.add(projectReferenceTypeProvider.createType());
			additionalTypes.add(newPageType(PROJECT_REFERENCE_PAGE_TYPE_NAME, PROJECT_REFERENCE_TYPE_NAME));

			additionalTypes.add(tagTypeProvider.createType());
			additionalTypes.add(newPageType(TAG_PAGE_TYPE_NAME, TAG_TYPE_NAME));

			additionalTypes.add(tagFamilyTypeProvider.createType());
			additionalTypes.add(newPageType(TAG_FAMILY_PAGE_TYPE_NAME, TAG_FAMILY_TYPE_NAME));

			additionalTypes.add(userTypeProvider.createType());
			additionalTypes.add(newPageType(USER_PAGE_TYPE_NAME, USER_TYPE_NAME));

			additionalTypes.add(groupTypeProvider.createType());
			additionalTypes.add(newPageType(GROUP_PAGE_TYPE_NAME, GROUP_TYPE_NAME));

			additionalTypes.add(roleTypeProvider.createType());
			additionalTypes.add(newPageType(ROLE_PAGE_TYPE_NAME, ROLE_TYPE_NAME));

			additionalTypes.add(branchTypeProvider.createType());

			additionalTypes.add(pluginProvider.createType());
			additionalTypes.add(newPageType(PLUGIN_PAGE_TYPE_NAME, PLUGIN_TYPE_NAME));

			additionalTypes.add(meshTypeProvider.createType());
			additionalTypes.add(interfaceTypeProvider.createPermInfoType());
			additionalTypes.add(fieldDefProvider.createBinaryFieldType());
			additionalTypes.add(fieldDefProvider.createS3BinaryFieldType());

			Versioned.doSince(2, context, () -> {
				List<GraphQLObjectType> schemaFieldTypes = context.getOrStore(NodeTypeProvider.SCHEMA_FIELD_TYPES, () -> nodeTypeProvider.generateSchemaFieldTypes(context).forVersion(context));
				if (CollectionUtils.isNotEmpty(schemaFieldTypes)) {
					additionalTypes.addAll(schemaFieldTypes);
				}
				List<GraphQLObjectType> microschemaFieldTypes = context.getOrStore(MicronodeFieldTypeProvider.MICROSCHEMA_FIELD_TYPES, () -> micronodeFieldTypeProvider.generateMicroschemaFieldTypes(context).forVersion(context));

				if (CollectionUtils.isNotEmpty(microschemaFieldTypes)) {
					additionalTypes.addAll(microschemaFieldTypes);
				}
			});

			// Shared argument types
			additionalTypes.add(createLinkEnumType());
			additionalTypes.add(createNodeEnumType());
			additionalTypes.add(createNativeFilterEnumType());
			additionalTypes.add(Sorting.getSortingEnumType());

			GraphQLSchema schema = builder.query(getRootType(context)).additionalTypes(additionalTypes).build();
			return schema;
		});
	}

	/**
	 * Get the cache key for the context.
	 * The cache key consists of
	 * <ol>
	 * <li>Project UUID</li>
	 * <li>Branch UUID</li>
	 * <li>API Version</li>
	 * </ol>
	 * @param context graphql context
	 * @return cache key
	 */
	protected String getCacheKey(GraphQLContext context) {
		HibProject project = Tx.get().getProject(context);
		HibBranch branch = Tx.get().getBranch(context);

		return String.format("%s-%s-%d", project.getUuid(), branch.getUuid(), context.getApiVersion());
	}
}
