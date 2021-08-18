package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_TYPE_NAME;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

/**
 * GraphQL type provider for project types.
 */
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
		ContentDaoWrapper contentDao = (ContentDaoWrapper) Tx.get().contentDao();
		GraphQLContext gc = env.getContext();
		HibProject project = env.getSource();
		HibNode node = project.getBaseNode();
		gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
		List<String> languageTags = getLanguageArgument(env);
		ContainerType type = getNodeVersion(env);

		NodeGraphFieldContainer container = contentDao.findVersion(node, gc, languageTags, type);
		container = gc.requiresReadPermSoft(container, env);
		return new NodeContent(node, container, languageTags, type);
	}

	/**
	 * Create the type definition.
	 * 
	 * @param project
	 * @return
	 */
	public GraphQLObjectType createType(HibProject project) {
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
				.argument(createNodeVersionArg())
				.dataFetcher(this::baseNodeFetcher));

		return root.build();
	}

}
