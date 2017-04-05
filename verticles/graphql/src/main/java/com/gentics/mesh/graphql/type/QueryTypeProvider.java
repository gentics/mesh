package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandler;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLSchema;
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
	public NodeFieldTypeProvider nodeFieldTypeProvider;

	@Inject
	public NodeTypeProvider nodeTypeProvider;

	@Inject
	public ProjectTypeProvider projectTypeProvider;

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
	public ContentTypeProvider contentTypeProvider;

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
			Node node = boot.nodeRoot()
					.findByUuid(uuid);
			return gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
		}
		return null;
	}

	/**
	 * Data fetcher for the content.
	 *
	 * @param env
	 * @return
	 */
	public Object contentFetcher(DataFetchingEnvironment env) {
		String path = env.getArgument("path");
		if (path != null) {
			GraphQLContext gc = env.getContext();
			Path pathResult = webrootService.findByProjectPath(gc, path);
			return pathResult.getLast()
					.getContainer();
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
			return gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
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
		root.field(newFieldDefinition().name("me")
				.description("The current user")
				.type(userTypeProvider.getUserType())
				.dataFetcher(this::userMeFetcher)
				.build());

		//.project
		root.field(newFieldDefinition().name("project")
				.description("Load the project that is active for this graphql query.")
				.type(projectTypeProvider.createProjectType(project))
				.dataFetcher(this::projectFetcher)
				.build());

		// NOT ALLOWED - See class description for details
		// .projects
		//	root.field(newPagingField("projects", "Load page of projects.", (ac) -> boot.projectRoot(), "Project"));

		// .node
		root.field(newFieldDefinition().name("node")
				.description("Load a node by uuid.")
				.argument(createUuidArg("Node uuid"))
				.dataFetcher(this::nodeFetcher)
				.type(nodeTypeProvider.createNodeType(project))
				.build());

		// .content
		root.field(newFieldDefinition().name("content")
				.description("Load a node content by its webroot path.")
				.argument(createPathArg())
				.type(contentTypeProvider.createContentType(project))
				.dataFetcher(this::contentFetcher));

		// .contents
		root.field(newFieldDefinition().name("contents")
				.description("Search for node contents and return a page which includes the results.")
				.argument(getPagingArgs())
				.argument(getQueryArg())
				.type(newPageType("contents", new GraphQLTypeReference("Content")))
				.dataFetcher((env) -> {
					GraphQLContext gc = env.getContext();

					PagingParameters pagingInfo = getPagingInfo(env);
					String query = env.getArgument("query");
					if (query != null) {
						return contentTypeProvider.handleContentSearch(gc, query, pagingInfo);
					}
					return null;
				}));

		// .nodes
		root.field(newFieldDefinition().name("nodes")
				.description("Load a page of nodes.")
				.argument(getPagingArgs())
				.argument(getQueryArg())
				.type(newPageType("nodes", new GraphQLTypeReference("Node")))
				.dataFetcher((env) -> {
					GraphQLContext gc = env.getContext();

					PagingParameters pagingInfo = getPagingInfo(env);
					String query = env.getArgument("query");
					if (query != null) {
						return nodeTypeProvider.handleSearch(gc, query, pagingInfo);
					} else {
						NodeRoot nodeRoot = gc.getProject()
								.getNodeRoot();
						return nodeRoot.findAll(gc, pagingInfo);
					}
				}));

		// .baseNode
		root.field(newFieldDefinition().name("rootNode")
				.description("Return the project root node.")
				.type(new GraphQLTypeReference("Node"))
				.dataFetcher(this::rootNodeFetcher)
				.build());

		// .tag
		root.field(newElementField("tag", "Load tag by name or uuid.", (ac) -> boot.tagRoot(), tagTypeProvider.createTagType()));

		// .tags
		root.field(newPagingSearchField("tags", "Load page of tags.", (ac) -> boot.tagRoot(), "Tag", tagIndexHandler));

		// .tagFamily
		root.field(newElementField("tagFamily", "Load tagFamily by name or uuid.", (ac) -> ac.getProject()
				.getTagFamilyRoot(), tagFamilyTypeProvider.getTagFamilyType()));

		// .tagFamilies
		root.field(
				newPagingSearchField("tagFamilies", "Load page of tagFamilies.", (ac) -> boot.tagFamilyRoot(), "TagFamily", tagFamilyIndexHandler));

		// .release
		root.field(newElementField("release", "Load release by name or uuid.", (ac) -> ac.getProject()
				.getReleaseRoot(), releaseTypeProvider.getReleaseType()));

		//.releases
		root.field(newPagingField("releases", "Load page of releases.", (ac) -> ac.getProject()
				.getReleaseRoot(), "Release"));

		// .schema
		root.field(newElementField("schema", "Load schema by name or uuid.", (ac) -> boot.schemaContainerRoot(), schemaTypeProvider.getSchemaType()));

		// .schemas
		root.field(newPagingField("schemas", "Load page of schemas.", (ac) -> boot.schemaContainerRoot(), "Schema"));

		// .microschema
		root.field(newElementField("microschema", "Load microschema by name or uuid.", (ac) -> boot.microschemaContainerRoot(),
				microschemaTypeProvider.createMicroschemaType()));

		// .microschemas
		root.field(newPagingField("microschemas", "Load page of microschemas.", (ac) -> boot.microschemaContainerRoot(), "Microschema"));

		// .role
		root.field(newElementField("role", "Load role by name or uuid.", (ac) -> boot.roleRoot(), roleTypeProvider.createRoleType()));

		// .roles
		root.field(newPagingSearchField("roles", "Load page of roles.", (ac) -> boot.roleRoot(), "Role", roleIndexHandler));

		// .group
		root.field(newElementField("group", "Load group by name or uuid.", (ac) -> boot.groupRoot(), groupTypeProvider.createGroupType()));

		// .groups
		root.field(newPagingSearchField("groups", "Load page of groups.", (ac) -> boot.groupRoot(), "Group", groupIndexHandler));

		// .user
		root.field(newElementField("user", "Load user by name or uuid.", (ac) -> boot.userRoot(), userTypeProvider.getUserType()));

		// .users
		root.field(newPagingSearchField("users", "Load page of users.", (ac) -> boot.userRoot(), "User", userIndexHandler));

		// .mesh
		root.field(meshTypeProvider.createMeshFieldType());

		return root.build();
	}

	/**
	 * Construct the root schema.
	 * 
	 * @param project
	 * @return
	 */
	public GraphQLSchema getRootSchema(Project project) {
		graphql.schema.GraphQLSchema.Builder schema = GraphQLSchema.newSchema();
		return schema.query(getRootType(project))
				.build();
	}

}
