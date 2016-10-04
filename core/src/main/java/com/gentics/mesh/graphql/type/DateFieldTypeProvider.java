package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import graphql.schema.GraphQLObjectType;

@Singleton
public class DateFieldTypeProvider {

	private Lazy<NodeFieldTypeProvider> nodeFieldTypeProvider;

	@Inject
	public DateFieldTypeProvider(Lazy<NodeFieldTypeProvider> nodeFieldTypeProvider) {
		this.nodeFieldTypeProvider = nodeFieldTypeProvider;
	}

	public GraphQLObjectType getDateFieldType() {
		GraphQLObjectType dateFieldType = newObject().name("date").withInterface(nodeFieldTypeProvider.get().getFieldType())
				.field(newFieldDefinition().name("name").type(GraphQLString).build())
				.field(newFieldDefinition().name("value").type(GraphQLString).build()).build();
		return dateFieldType;
	}
}
