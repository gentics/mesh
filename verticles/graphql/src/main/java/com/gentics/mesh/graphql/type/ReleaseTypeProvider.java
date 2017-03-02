package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class ReleaseTypeProvider extends AbstractTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public ReleaseTypeProvider() {
	}

	public GraphQLObjectType getReleaseType() {
		Builder releaseType = newObject().name("Release");
		interfaceTypeProvider.addCommonFields(releaseType);
		releaseType.field(newFieldDefinition().name("name")
				.type(GraphQLString)
				.build());
		return releaseType.build();
	}

}
