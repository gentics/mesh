package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.graphql.type.GroupTypeProvider.GROUP_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.GroupTypeProvider.GROUP_TYPE_NAME;
import static com.gentics.mesh.graphql.type.MicroschemaTypeProvider.MICROSCHEMA_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.MicroschemaTypeProvider.MICROSCHEMA_TYPE_NAME;
import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.ProjectReferenceTypeProvider.PROJECT_REFERENCE_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.ProjectReferenceTypeProvider.PROJECT_REFERENCE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.ProjectTypeProvider.PROJECT_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.ProjectTypeProvider.PROJECT_TYPE_NAME;
import static com.gentics.mesh.graphql.type.ReleaseTypeProvider.RELEASE_TYPE_NAME;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.WrappedPageImpl;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.type.field.FieldDefinitionProvider;
import com.gentics.mesh.graphql.type.field.MicronodeFieldTypeProvider;
import com.gentics.mesh.graphql.type.field.NodeFieldTypeProvider;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;

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
 * and content schema v2 of projectB. It is not possible to access data across releases due to the same reason. A different release may use different schema
 * versions.
 */
@Singleton
public class QueryTypeProvider extends AbstractTypeProvider {

	@Inject
	public MeshTypeProvider meshTypeProvider;

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public NodeFieldTypeProvider nodeFieldTypeProvider;

	@Inject
	public MicronodeFieldTypeProvider micronodeFieldTypeProvider;

	@Inject
	public FieldDefinitionProvider fieldDefProvider;

	@Inject
	public NodeTypeProvider nodeTypeProvider;

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
	public ReleaseTypeProvider releaseTypeProvider;

	@Inject
	public SchemaTypeProvider schemaTypeProvider;

	@Inject
	public MicroschemaTypeProvider microschemaTypeProvider;

	@Inject
	public UserIndexHandler userIndexHandler;

	@Inject
	public RoleIndexHandler roleIndexHandler;

	@Inject
	public GroupIndexHandler groupIndexHandler;

	@Inject
	public ProjectIndexHandler projectIndexHandler;

	@Inject
	public TagFamilyIndexHandler tagFamilyIndexHandler;

	@Inject
	public TagIndexHandler tagIndexHandler;

	@Inject
	public QueryTypeProvider() {
	}

	/**
	 * Data fetcher for nodes.
	 * 
	 * @param env
	 * @return
	 */
	public Object nodeFetcher(DataFetchingEnvironment env) {
		String uuid = env.getArgument("uuid");
		if (uuid != null) {
			GraphQLContext gc = env.getContext();
			Node node = boot.nodeRoot().findByUuid(uuid);
			if (node == null) {
				// TODO Throw graphql aware not found exception
				return null;
			}
			node = gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
			List<String> languageTags = getLanguageArgument(env);
			NodeGraphFieldContainer container = node.findNextMatchingFieldContainer(gc, languageTags);
			return new NodeContent(node, container);
		}
		String path = env.getArgument("path");
		if (path != null) {
			GraphQLContext gc = env.getContext();
			Path pathResult = webrootService.findByProjectPath(gc, path);
			NodeGraphFieldContainer container = pathResult.getLast().getContainer();
			Node nodeOfContainer = container.getParentNode();
			nodeOfContainer = gc.requiresPerm(nodeOfContainer, READ_PERM, READ_PUBLISHED_PERM);
			return new NodeContent(nodeOfContainer, container);
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
		User requestUser = gc.getUser();
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
		Project project = gc.getProject();
		return gc.requiresPerm(project, READ_PERM);
	}

	/**
	 * Data fetcher for the current release.
	 * @param env
	 * @return
	 */
	public Object releaseFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		Release release = gc.getRelease();
		return gc.requiresPerm(release, READ_PERM);
	}

	/**
	 * Data fetcher for the root node of the current project.
	 * 
	 * @param env
	 * @return
	 */
	public Object rootNodeFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		Project project = gc.getProject();
		if (project != null) {
			Node node = project.getBaseNode();
			gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
			NodeGraphFieldContainer container = node.findNextMatchingFieldContainer(gc, getLanguageArgument(env));
			return new NodeContent(node, container);
		}
		return null;
	}

	/**
	 * Construct the query/root type for the current project.
	 * 
	 * @param project
	 * @return
	 */
	public GraphQLObjectType getRootType(Project project) {
		Builder root = newObject();
		root.name("Query");

		// .me
		root.field(newFieldDefinition().name("me").description("The current user").type(new GraphQLTypeReference(USER_TYPE_NAME))
				.dataFetcher(this::userMeFetcher).build());

		// .project
		root.field(newFieldDefinition().name("project").description("Load the project that is active for this GraphQL query.")
				.type(new GraphQLTypeReference(PROJECT_TYPE_NAME)).dataFetcher(this::projectFetcher).build());

		// NOT ALLOWED - See class description for details
		// .projects
		// root.field(newPagingField("projects", "Load page of projects.", (ac) -> boot.projectRoot(), "Project"));

		// .node
		root.field(newFieldDefinition().name("node").description("Load a node by uuid, uuid and language or webroot path.")
				.argument(createUuidArg("Node uuid")).argument(createLanguageTagArg()).argument(createPathArg()).dataFetcher(this::nodeFetcher)
				.type(new GraphQLTypeReference(NODE_TYPE_NAME)).build());

		// .nodes
		root.field(newFieldDefinition().name("nodes").description("Load a page of nodes via the regular nodes list or via a search.")
				.argument(createPagingArgs()).argument(createQueryArg()).argument(createLanguageTagArg())
				.type(new GraphQLTypeReference(NODE_PAGE_TYPE_NAME)).dataFetcher((env) -> {
					GraphQLContext gc = env.getContext();
					PagingParameters pagingInfo = getPagingInfo(env);
					String query = env.getArgument("query");

					// Check whether we need to load the nodes via a query or regular project-wide paging
					if (query != null) {
						return nodeTypeProvider.handleContentSearch(gc, query, pagingInfo);
					} else {
						NodeRoot nodeRoot = gc.getProject().getNodeRoot();
						TransformablePage<? extends Node> nodes = nodeRoot.findAll(gc, pagingInfo);

						// Now lets try to load the containers for those found nodes - apply the language fallback
						List<String> languageTags = getLanguageArgument(env);
						List<NodeContent> contents = nodes.getWrappedList().stream().map(node -> {
							NodeGraphFieldContainer container = node.findNextMatchingFieldContainer(gc, languageTags);
							return new NodeContent(node, container);
						}).collect(Collectors.toList());
						return new WrappedPageImpl<NodeContent>(contents, nodes);
					}
				}));

		// .baseNode
		root.field(newFieldDefinition().name("rootNode").description("Return the project root node.").argument(createLanguageTagArg())
				.type(new GraphQLTypeReference(NODE_TYPE_NAME)).dataFetcher(this::rootNodeFetcher).build());

		// .tag
		root.field(newElementField("tag", "Load tag by name or uuid.", (ac) -> boot.tagRoot(), TAG_TYPE_NAME));

		// .tags
		root.field(newPagingSearchField("tags", "Load page of tags.", (ac) -> boot.tagRoot(), TAG_PAGE_TYPE_NAME, tagIndexHandler));

		// .tagFamily
		root.field(newElementField("tagFamily", "Load tagFamily by name or uuid.", (ac) -> ac.getProject().getTagFamilyRoot(), TAG_FAMILY_TYPE_NAME));

		// .tagFamilies
		root.field(newPagingSearchField("tagFamilies", "Load page of tagFamilies.", (ac) -> boot.tagFamilyRoot(), TAG_FAMILY_PAGE_TYPE_NAME,
				tagFamilyIndexHandler));

		// .release
		root.field(newFieldDefinition().name("release").description("Load the release that is active for this GraphQL query.")
				.type(new GraphQLTypeReference(RELEASE_TYPE_NAME)).dataFetcher(this::releaseFetcher).build());

		// .schema
		root.field(newElementField("schema", "Load schema by name or uuid.", (ac) -> boot.schemaContainerRoot(), SCHEMA_TYPE_NAME));

		// .schemas
		root.field(newPagingField("schemas", "Load page of schemas.", (ac) -> boot.schemaContainerRoot(), SCHEMA_PAGE_TYPE_NAME));

		// .microschema
		root.field(
				newElementField("microschema", "Load microschema by name or uuid.", (ac) -> boot.microschemaContainerRoot(), MICROSCHEMA_TYPE_NAME));

		// .microschemas
		root.field(newPagingField("microschemas", "Load page of microschemas.", (ac) -> boot.microschemaContainerRoot(), MICROSCHEMA_PAGE_TYPE_NAME));

		// .role
		root.field(newElementField("role", "Load role by name or uuid.", (ac) -> boot.roleRoot(), ROLE_TYPE_NAME));

		// .roles
		root.field(newPagingSearchField("roles", "Load page of roles.", (ac) -> boot.roleRoot(), ROLE_PAGE_TYPE_NAME, roleIndexHandler));

		// .group
		root.field(newElementField("group", "Load group by name or uuid.", (ac) -> boot.groupRoot(), GROUP_TYPE_NAME));

		// .groups
		root.field(newPagingSearchField("groups", "Load page of groups.", (ac) -> boot.groupRoot(), GROUP_PAGE_TYPE_NAME, groupIndexHandler));

		// .user
		root.field(newElementField("user", "Load user by name or uuid.", (ac) -> boot.userRoot(), USER_TYPE_NAME));

		// .users
		root.field(newPagingSearchField("users", "Load page of users.", (ac) -> boot.userRoot(), USER_PAGE_TYPE_NAME, userIndexHandler));

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

		type.field(newFieldDefinition().name("unfilteredCount")
				.description(
						"Return the unfiltered total count. Note that this value may not be exact since no permission filtering will be applied.")
				.dataFetcher(env -> {
					Page<?> page = env.getSource();
					return page.getUnfilteredSearchCount();
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
	 * @param project
	 * @return
	 */
	public GraphQLSchema getRootSchema(Project project) {
		graphql.schema.GraphQLSchema.Builder builder = GraphQLSchema.newSchema();

		Set<GraphQLType> additionalTypes = new HashSet<>();

		additionalTypes.add(schemaTypeProvider.createType());
		additionalTypes.add(newPageType(SCHEMA_PAGE_TYPE_NAME, SCHEMA_TYPE_NAME));

		additionalTypes.add(microschemaTypeProvider.createType());
		additionalTypes.add(newPageType(MICROSCHEMA_PAGE_TYPE_NAME, MICROSCHEMA_TYPE_NAME));

		additionalTypes.add(nodeTypeProvider.createType(project));
		additionalTypes.add(newPageType(NODE_PAGE_TYPE_NAME, NODE_TYPE_NAME));

		additionalTypes.add(micronodeFieldTypeProvider.createType(project));

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

		additionalTypes.add(releaseTypeProvider.createType());

		additionalTypes.add(meshTypeProvider.createType());
		additionalTypes.add(interfaceTypeProvider.createPermInfoType());
		additionalTypes.add(fieldDefProvider.createBinaryFieldType());

		additionalTypes.add(createLinkEnumType());

		GraphQLSchema schema = builder.query(getRootType(project)).build(additionalTypes);
		return schema;
	}

}
