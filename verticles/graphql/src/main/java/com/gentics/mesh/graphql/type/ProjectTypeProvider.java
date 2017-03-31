package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class ProjectTypeProvider extends AbstractTypeProvider {

	@Inject
	public NodeTypeProvider nodeTypeProvider;

	@Inject
	public UserTypeProvider userTypeProvider;

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public ProjectTypeProvider() {
	}

	/**
	 * Fetcher for the project base node.
	 * 
	 * @param env
	 * @return
	 */
	private Node baseNodeFetcher(DataFetchingEnvironment env) {
		GraphQLContext gc = env.getContext();
		Project project = env.getSource();
		Node node = project.getBaseNode();
		return gc.requiresPerm(node, READ_PERM, READ_PUBLISHED_PERM);
	}

	public GraphQLObjectType createProjectType(Project project) {
		Builder root = newObject();
		root.name("Project");
		interfaceTypeProvider.addCommonFields(root);

		// .name
		root.field(newFieldDefinition().name("name")
				.description("The name of the project")
				.type(GraphQLString));

		// .rootNode
		root.field(newFieldDefinition().name("rootNode")
				.description("The root node of the project")
				.type(nodeTypeProvider.createNodeType(project))
				.dataFetcher(this::baseNodeFetcher));

		return root.build();
	}

}
