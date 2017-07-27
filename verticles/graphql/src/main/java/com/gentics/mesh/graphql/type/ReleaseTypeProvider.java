package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class ReleaseTypeProvider extends AbstractTypeProvider {

	public static final String RELEASE_TYPE_NAME = "Release";

	public static final String RELEASE_PAGE_TYPE_NAME = "ReleasesPages";

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public ReleaseTypeProvider() {
	}

	public GraphQLObjectType createType() {
		Builder releaseType = newObject().name(RELEASE_TYPE_NAME);
		interfaceTypeProvider.addCommonFields(releaseType);

		// .name
		releaseType.field(newFieldDefinition().name("name").type(GraphQLString));

		// .migrated
		releaseType.field(newFieldDefinition().name("migrated").type(GraphQLBoolean));

		// TODO add more fields

		return releaseType.build();
	}

}
