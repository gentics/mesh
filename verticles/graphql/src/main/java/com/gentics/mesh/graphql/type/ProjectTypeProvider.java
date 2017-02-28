package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class ProjectTypeProvider {

	@Inject
	public NodeTypeProvider nodeTypeProvider;

	@Inject
	public UserTypeProvider userTypeProvider;

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public ProjectTypeProvider() {
	}

	public GraphQLObjectType getProjectType(Project project) {
		Builder root = newObject();
		root.name("Project");
		interfaceTypeProvider.addCommonFields(root);
		root.field(newFieldDefinition().name("name").description("The name of the project").type(GraphQLString).build());
		root.field(newFieldDefinition().name("rootNode").description("The root node of the project").type(nodeTypeProvider.getNodeType(project)).build());
		return root.build();
	}

}
