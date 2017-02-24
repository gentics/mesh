package com.gentics.mesh.graphql.type;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;

@Singleton
public class DateFieldTypeProvider {

	private Lazy<NodeFieldTypeProvider> nodeFieldTypeProvider;

	@Inject
	public DateFieldTypeProvider(Lazy<NodeFieldTypeProvider> nodeFieldTypeProvider) {
		this.nodeFieldTypeProvider = nodeFieldTypeProvider;
	}

//	public GraphQLObjectType getDateFieldType() {
//		GraphQLObjectType dateFieldType = newObject().name("date").withInterface(nodeFieldTypeProvider.get().getFieldsType())
//				.field(newFieldDefinition().name("name").type(GraphQLString).build())
//				.field(newFieldDefinition().name("value").type(GraphQLString).build()).build();
//		return dateFieldType;
//	}
}
