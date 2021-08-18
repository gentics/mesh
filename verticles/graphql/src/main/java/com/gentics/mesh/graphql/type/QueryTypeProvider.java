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
import static graphql.Scalars.GraphQLLong;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.action.DAOActionsCollection;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.PermissionException;
import com.gentics.mesh.core.rest.error.UuidNotFoundException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.GroupFilter;
import com.gentics.mesh.graphql.filter.NodeFilter;
import com.gentics.mesh.graphql.filter.RoleFilter;
import com.gentics.mesh.graphql.filter.UserFilter;
import com.gentics.mesh.graphql.type.field.FieldDefinitionProvider;
import com.gentics.mesh.graphql.type.field.MicronodeFieldTypeProvider;
import com.gentics.mesh.handler.Versioned;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.impl.PathSegmentImpl;
import com.gentics.mesh.search.index.group.GroupSearchHandler;
import com.gentics.mesh.search.index.project.ProjectSearchHandler;
import com.gentics.mesh.search.index.role.RoleSearchHandler;
import com.gentics.mesh.search.index.tag.TagSearchHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilySearchHandler;
import com.gentics.mesh.search.index.user.UserSearchHandler;

import graphql.ExceptionWhileDataFetching;
import graphql.execution.ExecutionContext;
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

	@Inject
	public MeshTypeProvider meshTypeProvider;

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public MicronodeFieldTypeProvider micronodeFieldTypeProvider;

	@Inject
	public FieldDefinitionProvider fieldDefProvider;

	@Inject
	public NodeTypeProvider nodeTypeProvider;

	@Inject
	public NodeReferenceTypeProvider nodeReferenceTypeProvider;

	@Inject
	public ProjectTypeProvider projectTypeProvider;

	@Inject
	public ProjectReferenceTypeProvider projectReferenceTypeProvider;

	@Inject
	public UserTypeProvider userTypeProvider;

	@Inject
	public TagTypeProvider tagTypeProvider;

	@Inject
	public TagFamilyTypeProvider tagFamilyTypeProvider;

	@Inject
	public RoleTypeProvider roleTypeProvider;

	@Inject
	public GroupTypeProvider groupTypeProvider;

	@Inject
	public WebRootService webrootService;

	@Inject
	public BootstrapInitializer boot;

	@Inject
	public BranchTypeProvider branchTypeProvider;

	@Inject
	public SchemaTypeProvider schemaTypeProvider;

	@Inject
	public MicroschemaTypeProvider microschemaTypeProvider;

	@Inject
	public UserSearchHandler userSearchHandler;

	@Inject
	public RoleSearchHandler roleSearchHandler;

	@Inject
	public GroupSearchHandler groupSearchHandler;

	@Inject
	public ProjectSearchHandler projectSearchHandler;

	@Inject
	public TagFamilySearchHandler tagFamilySearchHandler;

	@Inject
	public TagSearchHandler tagSearchHandler;

	@Inject
	public PluginTypeProvider pluginProvider;

	@Inject
	public PluginApiTypeProvider pluginApiProvider;

	@Inject
	public DAOActionsCollection actions;

	@Inject
	public QueryTypeProvider(MeshOptions options) {
		super(options);
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
	private Page<NodeContent> fetchNodesByUuid(DataFetchingEnvironment env) {
		Tx tx = Tx.get();
		ContentDaoWrapper contentDao = (ContentDaoWrapper) tx.contentDao();
		NodeDao nodeDao = tx.nodeDao();

		List<String> uuids = env.getArgument("uuids");

		if (uuids == null || uuids.isEmpty()) {
			return new DynamicStreamPageImpl<>(Stream.empty(), getPagingInfo(env));
		}

		GraphQLContext gc = env.getContext();
		HibProject project = tx.getProject(gc);
		ExecutionContext ec = env.getExecutionContext();
		List<String> languageTags = getLanguageArgument(env);
		ContainerType type = getNodeVersion(env);

		Stream<NodeContent> contents = uuids.stream()
			// When a node cannot be found, we still need the UUID for the error message.
			.map(uuid -> Pair.of(uuid, nodeDao.findByUuid(project, uuid)))
			.map(node -> {
				Throwable error = null;

				if (node.getRight() == null) {
					error = new UuidNotFoundException("node", node.getLeft());
				} else {
					// The node was found, check the permissions.
					try {
						return (Node) gc.requiresPerm(node.getRight(), READ_PERM, READ_PUBLISHED_PERM);
					} catch (PermissionException e) {
						error = e;
					}
				}

				ec.addError(new ExceptionWhileDataFetching(env.getFieldTypeInfo().getPath(), error, env.getField().getSourceLocation()));

				return null;
			})
			.filter(Objects::nonNull)
			.map(node -> {
				NodeGraphFieldContainer container = contentDao.findVersion(node, gc, languageTags, type);
				return new NodeContent(node, container, languageTags, type);
			})
			.filter(content -> content.getContainer() != null)
			.filter(gc::hasReadPerm);

		return applyNodeFilter(env, contents);
	}

	/**
	 * Data fetcher for nodes.
	 * 
	 * @param env
	 * @return
	 */
	public Object nodeFetcher(DataFetchingEnvironment env) {
		Tx tx = Tx.get();
		ContentDaoWrapper contentDao = (ContentDaoWrapper) tx.contentDao();
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
			NodeGraphFieldContainer container = contentDao.findVersion(node, gc, languageTags, type);
			if (container != null) {
				container = gc.requiresReadPermSoft(container, env);
			}
			return new NodeContent(node, container, languageTags, type);
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
			PathSegmentImpl graphSegment = (PathSegmentImpl) pathResult.getLast();
			NodeGraphFieldContainer container = graphSegment.getContainer();
			HibNode nodeOfContainer = contentDao.getNode(container);

			nodeOfContainer = gc.requiresPerm(nodeOfContainer, READ_PERM, READ_PUBLISHED_PERM);
			container = gc.requiresReadPermSoft(container, env);
			List<String> langs = new ArrayList<>();
			if (container != null) {
				langs.add(container.getLanguageTag());
			}
			return new NodeContent(nodeOfContainer, container, langs, type);
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
		ContentDaoWrapper contentDao = (ContentDaoWrapper) tx.contentDao();
		GraphQLContext gc = env.getContext();
		HibProject project = tx.getProject(gc);
		if (project != null) {
			HibNode node = project.getBaseNode();
			gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
			List<String> languageTags = getLanguageArgument(env);
			ContainerType type = getNodeVersion(env);
			NodeGraphFieldContainer container = contentDao.findVersion(node, gc, languageTags, type);
			container = gc.requiresReadPermSoft(container, env);
			return new NodeContent(node, container, languageTags, type);
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
		root.field(newFieldDefinition().name("nodes")
			.description("Load a page of nodes via the regular nodes list or via a search.")
			.argument(createPagingArgs())
			.argument(createQueryArg())
			.argument(createUuidsArg("Node uuids"))
			.argument(createLanguageTagArg(true))
			.argument(createNodeVersionArg())
			.argument(NodeFilter.filter(context).createFilterArgument())
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
		HibProject project = Tx.get().getProject(context);
		graphql.schema.GraphQLSchema.Builder builder = GraphQLSchema.newSchema();

		Set<GraphQLType> additionalTypes = new HashSet<>();

		additionalTypes.add(schemaTypeProvider.createType(context));
		additionalTypes.add(newPageType(SCHEMA_PAGE_TYPE_NAME, SCHEMA_TYPE_NAME));

		additionalTypes.add(microschemaTypeProvider.createType());
		additionalTypes.add(newPageType(MICROSCHEMA_PAGE_TYPE_NAME, MICROSCHEMA_TYPE_NAME));

		additionalTypes.add(nodeTypeProvider.createVersionInfoType());
		additionalTypes.add(nodeTypeProvider.createType(context).forVersion(context));
		additionalTypes.add(newPageType(NODE_PAGE_TYPE_NAME, NODE_TYPE_NAME));

		additionalTypes.add(nodeReferenceTypeProvider.createType());
		additionalTypes.add(newPageType(NODE_REFERENCE_PAGE_TYPE_NAME, NODE_REFERENCE_TYPE_NAME));

		additionalTypes.add(micronodeFieldTypeProvider.createType(context).forVersion(context));

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

		// Shared argument types
		additionalTypes.add(createLinkEnumType());
		additionalTypes.add(createNodeEnumType());

		Versioned.doSince(2, context, () -> {
			additionalTypes.addAll(nodeTypeProvider.generateSchemaFieldTypes(context).forVersion(context));
			additionalTypes.addAll(micronodeFieldTypeProvider.generateMicroschemaFieldTypes(context).forVersion(context));
		});

		GraphQLSchema schema = builder.query(getRootType(context)).additionalTypes(additionalTypes).build();
		return schema;
	}

}
