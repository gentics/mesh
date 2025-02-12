package com.gentics.mesh.graphql.type;

import graphql.schema.GraphQLObjectType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * GraphQL type provider for project reference types.
 */
@Singleton
public class ProjectReferenceTypeProvider extends AbstractTypeProvider {
	public static final String PROJECT_REFERENCE_TYPE_NAME = "ProjectReference";

	public static final String PROJECT_REFERENCE_PAGE_TYPE_NAME = "ProjectReferencesPage";

	protected final InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public ProjectReferenceTypeProvider(MeshOptions options, InterfaceTypeProvider interfaceTypeProvider) {
		super(options);
		this.interfaceTypeProvider = interfaceTypeProvider;
	}

	public GraphQLObjectType createType() {
		GraphQLObjectType.Builder root = newObject();
		root.name(PROJECT_REFERENCE_TYPE_NAME);
		interfaceTypeProvider.addCommonFields(root);

		// .name
		root.field(newFieldDefinition().name("name").description("The name of the project").type(GraphQLString));

		return root.build();
	}
}
