package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_TYPE_NAME;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class ProjectTypeProvider extends AbstractTypeProvider {

	public static final String PROJECT_TYPE_NAME = "Project";

	public static final String PROJECT_PAGE_TYPE_NAME = "ProjectsPage";

	@Inject
	public NodeTypeProvider nodeTypeProvider;

	@Inject
	public UserTypeProvider userTypeProvider;

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public ProjectTypeProvider(MeshOptions options) {
		super(options);
	}

	/**
	 * Fetcher for the project base node.
	 * 
	 * @param env
	 * @return
	 */
	private NodeContent baseNodeFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		Project project = env.getSource();
		Node node = project.getBaseNode();
		gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
		List<String> languageTags = getLanguageArgument(env);
		ContainerType type = getNodeContainerType(env);

		NodeGraphFieldContainer container = node.findVersion(gc, languageTags, type);
		container = gc.requiresReadPermSoft(container, env);
		return new NodeContent(node, container, languageTags);
	}

	public GraphQLObjectType createType(Project project) {
		Builder root = newObject();
		root.name(PROJECT_TYPE_NAME);
		interfaceTypeProvider.addCommonFields(root);

		// .name
		root.field(newFieldDefinition().name("name").description("The name of the project").type(GraphQLString));

		// .rootNode
		root.field(
			newFieldDefinition()
				.name("rootNode")
				.description("The root node of the project")
				.type(new GraphQLTypeReference(NODE_TYPE_NAME))
				.argument(createLanguageTagArg(true))
				.argument(createNodeTypeArg())
				.dataFetcher(this::baseNodeFetcher));

		return root.build();
	}

}
