package com.gentics.mesh.graphql.type;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.path.Path;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLSchema;

@Singleton
public class RootTypeProvider extends AbstractTypeProvider {

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
	public MicroschemaTypeProvider microschemaTypeProvider;

	@Inject
	public RootTypeProvider() {
	}

	public Object nodeFetcher(DataFetchingEnvironment env) {
		String uuid = env.getArgument("uuid");
		if (uuid != null) {
			InternalActionContext ac = (InternalActionContext) env.getContext();
			Node node = boot.nodeRoot()
					.findByUuid(uuid);
			// Check permissions
			if (ac.getUser()
					.hasPermission(node, GraphPermission.READ_PERM)
					|| ac.getUser()
							.hasPermission(node, GraphPermission.READ_PUBLISHED_PERM)) {
				return node;
			}
		}

		String path = env.getArgument("path");
		if (path != null) {
			InternalActionContext ac = (InternalActionContext) env.getContext();
			Path pathResult = webrootService.findByProjectPath(ac, path);
			return pathResult.getLast()
					.getNode();
		}
		return null;
	}

	public Object userMeFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof InternalActionContext) {
			InternalActionContext ac = (InternalActionContext) source;
			MeshAuthUser requestUser = ac.getUser();
			return requestUser;
		}
		return null;
	}

	public GraphQLObjectType getRootType(Project project) {
		Builder root = newObject();
		root.name("Mesh root");

		// .me
		root.field(newFieldDefinition().name("me")
				.description("The current user")
				.type(userTypeProvider.getUserType())
				.dataFetcher(this::userMeFetcher)
				.build());

		// .project
		root.field(
				newElementField("project", "Load project by name of uuid.", (ac) -> boot.projectRoot(), projectTypeProvider.getProjectType(project)));

		// .projects
		root.field(newPagingField("projects", "Load page of projects.", (ac) -> boot.projectRoot(), "Project"));

		// .node
		root.field(newElementField("node", "Load node by name of uuid.", (ac) -> boot.nodeRoot(), nodeTypeProvider.getNodeType(project)));

		// .nodes
		root.field(newPagingField("nodes", "Load page of nodes.", (ac) -> ac.getProject()
				.getTagFamilyRoot(), "Node"));

		// .tag
		root.field(newElementField("tag", "Load tag by name of uuid.", (ac) -> boot.tagRoot(), tagTypeProvider.getTagType()));

		// .tags
		root.field(newPagingField("tags", "Load page of tags.", (ac) -> boot.tagRoot(), "Tag"));

		// .tagFamily
		root.field(newElementField("tagFamily", "Load tagFamily by name of uuid.", (ac) -> ac.getProject()
				.getTagFamilyRoot(), tagFamilyTypeProvider.getTagFamilyType()));

		// .tagFamilies
		root.field(newPagingField("tagFamilies", "Load page of tagFamilies.", (ac) -> boot.tagFamilyRoot(), "TagFamily"));

		// .release
		root.field(newElementField("release", "Load release by name of uuid.", (ac) -> ac.getProject()
				.getReleaseRoot(), releaseTypeProvider.getReleaseType()));

		//.releases
		root.field(newPagingField("releases", "Load page of releases.", (ac) -> ac.getProject()
				.getReleaseRoot(), "Release"));

		// .schema
		root.field(newElementField("schema", "Load schema by name of uuid.", (ac) -> boot.schemaContainerRoot(), schemaTypeProvider.getSchemaType()));

		// .schemas
		root.field(newPagingField("schemas", "Load page of schemas.", (ac) -> boot.schemaContainerRoot(), "Schema"));

		// .microschema
		root.field(newElementField("microschema", "Load microschema by name of uuid.", (ac) -> boot.microschemaContainerRoot(),
				microschemaTypeProvider.getMicroschemaType()));

		// .microschemas
		root.field(newPagingField("microschemas", "Load page of microschemas.", (ac) -> boot.microschemaContainerRoot(), "Microschema"));

		// .role
		root.field(newElementField("role", "Load role by name of uuid.", (ac) -> boot.roleRoot(), roleTypeProvider.getRoleType()));

		// .roles
		root.field(newPagingField("roles", "Load page of roles.", (ac) -> boot.roleRoot(), "Role"));

		// .group
		root.field(newElementField("group", "Load group by name of uuid.", (ac) -> boot.groupRoot(), groupTypeProvider.getGroupType()));

		// .groups
		root.field(newPagingField("groups", "Load page of groups.", (ac) -> boot.groupRoot(), "Group"));

		// .user
		root.field(newElementField("user", "Load user by name of uuid.", (ac) -> boot.userRoot(), userTypeProvider.getUserType()));

		// .users
		root.field(newPagingField("users", "Load page of users.", (ac) -> boot.userRoot(), "User"));

		return root.build();
	}

	public GraphQLSchema getRootSchema(Project project) {
		graphql.schema.GraphQLSchema.Builder schema = GraphQLSchema.newSchema();
		return schema.query(getRootType(project))
				.build();
	}

}
