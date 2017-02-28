package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class MicroschemaTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public MicroschemaTypeProvider() {

	}

	public GraphQLObjectType getMicroschemaType() {
		Builder schemaType = newObject().name("Microschema").description("Microschema");
		interfaceTypeProvider.addCommonFields(schemaType);
		schemaType.field(newFieldDefinition().name("name").type(GraphQLString).build());

		// TODO add fields

		return schemaType.build();
	}

}
