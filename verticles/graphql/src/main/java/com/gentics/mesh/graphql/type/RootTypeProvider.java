package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.path.Path;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLSchema;

@Singleton
public class RootTypeProvider {

	@Inject
	public NodeFieldTypeProvider nodeFieldProvider;

	@Inject
	public NodeTypeProvider nodeTypeProvider;

	@Inject
	public ProjectTypeProvider projectTypeProvider;

	@Inject
	public UserTypeProvider userFieldProvider;

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
	public RootTypeProvider() {
	}

	public GraphQLObjectType getRootType(Project project) {
		Builder root = newObject();
		root.name("Mesh root");

		// .me
		root.field(newFieldDefinition().name("me").description("The current user").type(userFieldProvider.getUserType())
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof InternalActionContext) {
						InternalActionContext ac = (InternalActionContext) source;
						MeshAuthUser requestUser = ac.getUser();
						return requestUser;
					}
					return null;
				}).build());

		// .projects
		root.field(newFieldDefinition().name("projects").description("Load a project")
				.argument(newArgument().name("uuid").type(GraphQLString).description("Project uuid").build())
				.dataFetcher(fetcher -> {
					String uuid = fetcher.getArgument("uuid");
					return boot.projectRoot().findByUuid(uuid);
				}).type(projectTypeProvider.getProjectType(project)).build());

		// .nodes
		GraphQLFieldDefinition nodeField = newFieldDefinition().name("nodes").description("Load a node")
				.argument(newArgument().name("uuid").type(GraphQLString).description("Node uuid").build())
				.argument(newArgument().name("path").type(GraphQLString).description("Node webroot path").build())
				.dataFetcher(fetcher -> {
					String uuid = fetcher.getArgument("uuid");
					if (uuid != null) {
						return boot.nodeRoot().findByUuid(uuid);
					}
					String path = fetcher.getArgument("path");
					if (path != null) {
						Path pathResult = webrootService.findByProjectPath(null, path);
						return pathResult.getLast().getNode();
					}
					return null;
				}).type(nodeTypeProvider.getNodeType(project)).build();
		root.field(nodeField);

		// // .tags
		// root.field(newFieldDefinition().name("tags").description("Load a tag").argument(newArgument().name("uuid").description("Tag uuid").build())
		// .dataFetcher(fetcher -> {
		// String uuid = fetcher.getArgument("uuid");
		// return MeshInternal.get().boot().tagRoot().findByUuid(uuid);
		// }).type(tagTypeProvider.getTagType()).build());
		//
		// // .tagFamilies
		// root.field(newFieldDefinition().name("tagFamilies").description("Load a tag family")
		// .argument(newArgument().name("uuid").description("TagFamily uuid").build()).dataFetcher(fetcher -> {
		// String uuid = fetcher.getArgument("uuid");
		// return MeshInternal.get().boot().tagFamilyRoot().findByUuid(uuid);
		// }).type(tagFamilyTypeProvider.getTagFamilyType()).build());

		// .releases

		// .schemas

		// .microschemas

		// .roles
		// root.field(newFieldDefinition().name("roles").description("Load a role").argument(newArgument().name("uuid").description("Role uuid").build())
		// .dataFetcher(fetcher -> {
		// String uuid = fetcher.getArgument("uuid");
		// return MeshInternal.get().boot().roleRoot().findByUuid(uuid);
		// }).type(roleTypeProvider.getRoleType()).build());
		//
		// // .groups
		// root.field(newFieldDefinition().name("groups").description("Load a group")
		// .argument(newArgument().name("uuid").description("Group uuid").build()).dataFetcher(fetcher -> {
		// String uuid = fetcher.getArgument("uuid");
		// return MeshInternal.get().boot().roleRoot().findByUuid(uuid);
		// }).type(groupTypeProvider.getGroupType()).build());
		//
		// // .users
		// root.field(newFieldDefinition().name("users").description("Load a user").argument(newArgument().name("uuid").description("User uuid").build())
		// .dataFetcher(fetcher -> {
		// String uuid = fetcher.getArgument("uuid");
		// return MeshInternal.get().boot().userRoot().findByUuid(uuid);
		// }).type(userFieldProvider.getUserType()).build());

		return root.build();
	}

	public GraphQLSchema getRootSchema(Project project) {

		// Builder obj = newObject().name("helloWorldQuery");
		// obj.field(newFieldDefinition().type(GraphQLString).name("hello").staticValue("world").build());
		// obj.field(newFieldDefinition().type(GraphQLString).name("mop").dataFetcher(env -> {
		// return "mopValue";
		// }).build());
		// GraphQLObjectType queryType = obj.build();

		// Builder userBuilder = newObject().name("User").description("The user");
		// userBuilder.field(newFieldDefinition().type(GraphQLString).name("name").staticValue("someName").build());
		// GraphQLObjectType userType = userBuilder.build();

		// GraphQLUnionType fieldType = newUnionType().name("Fields").possibleType(dateFieldType).possibleType(stringFieldType)
		// .typeResolver(new TypeResolver() {
		// @Override
		// public GraphQLObjectType getType(Object object) {
		// if(object instanceof StringTestField) {
		// return stringFieldType;
		// }
		// if(object instanceof DateTestField) {
		// return dateFieldType;
		// }
		// return stringFieldType;
		// }
		// }).build();
		graphql.schema.GraphQLSchema.Builder schema = GraphQLSchema.newSchema();
		return schema.query(getRootType(project)).build();

	}

}
