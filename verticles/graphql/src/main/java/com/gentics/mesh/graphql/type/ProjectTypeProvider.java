package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;

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

	public Object baseNodeFetcher(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof Project) {
			InternalActionContext ac = (InternalActionContext) env.getContext();
			Node node = ((Project) source).getBaseNode();
			;
			if (ac.getUser()
					.hasPermission(node, GraphPermission.READ_PERM)
					|| ac.getUser()
							.hasPermission(node, GraphPermission.READ_PUBLISHED_PERM)) {
				return node;
			}
		}
		return null;
	}

	public GraphQLObjectType getProjectType(Project project) {
		Builder root = newObject();
		root.name("Project");
		interfaceTypeProvider.addCommonFields(root);

		root.field(newFieldDefinition().name("name")
				.description("The name of the project")
				.type(GraphQLString)
				.build());

		root.field(newFieldDefinition().name("rootNode")
				.description("The root node of the project")
				.type(nodeTypeProvider.getNodeType(project))
				.dataFetcher(this::baseNodeFetcher)
				.build());
		return root.build();
	}

}
