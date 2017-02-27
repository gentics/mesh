package com.gentics.mesh.graphql.type.field;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.graphql.type.NodeFieldTypeProvider;

import dagger.Lazy;
import graphql.schema.GraphQLFieldDefinition;

@Singleton
public class DateFieldTypeProvider {

	private Lazy<NodeFieldTypeProvider> nodeFieldTypeProvider;

	@Inject
	public DateFieldTypeProvider(Lazy<NodeFieldTypeProvider> nodeFieldTypeProvider) {
		this.nodeFieldTypeProvider = nodeFieldTypeProvider;
	}

	public GraphQLFieldDefinition getFieldDefinition(String name, String label) {
		// TODO Auto-generated method stub
		return null;
	}

//	public GraphQLObjectType getDateFieldType() {
//		GraphQLObjectType dateFieldType = newObject().name("date").withInterface(nodeFieldTypeProvider.get().getFieldsType())
//				.field(newFieldDefinition().name("name").type(GraphQLString).build())
//				.field(newFieldDefinition().name("value").type(GraphQLString).build()).build();
//		return dateFieldType;
//	}
}
