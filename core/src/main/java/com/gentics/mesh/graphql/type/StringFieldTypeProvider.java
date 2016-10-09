package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import graphql.schema.GraphQLObjectType;

@Singleton
public class StringFieldTypeProvider {

	private Lazy<NodeFieldTypeProvider> nodeFieldTypeProvider;

	@Inject
	public StringFieldTypeProvider(Lazy<NodeFieldTypeProvider> nodeFieldTypeProvider) {
		this.nodeFieldTypeProvider = nodeFieldTypeProvider;
	}

	public GraphQLObjectType getStringFieldType() {
		GraphQLObjectType stringFieldType = newObject().name("string").withInterface(nodeFieldTypeProvider.get().getFieldsType())
				.field(newFieldDefinition().name("name").type(GraphQLString).build())
				.field(newFieldDefinition().name("encoded").type(GraphQLBoolean).build()).build();
		return stringFieldType;
	}

}
