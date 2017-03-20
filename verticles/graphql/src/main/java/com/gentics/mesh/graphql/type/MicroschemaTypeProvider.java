package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class MicroschemaTypeProvider extends AbstractTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public MicroschemaTypeProvider() {

	}

	public GraphQLObjectType createMicroschemaType() {
		Builder schemaType = newObject().name("Microschema")
				.description("Microschema");
		interfaceTypeProvider.addCommonFields(schemaType);

		// .name
		schemaType.field(newFieldDefinition().name("name")
				.type(GraphQLString));

		// .version
		schemaType.field(newFieldDefinition().name("version")
				.description("Version of the microschema.")
				.type(GraphQLInt));

		// .description
		schemaType.field(newFieldDefinition().name("description")
				.description("Description of the microschema.")
				.type(GraphQLString));

		// .fields

		// TODO add fields

		return schemaType.build();
	}

}
