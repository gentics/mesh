package com.gentics.mesh.graphql.type;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;

@Singleton
public class StringFieldTypeProvider {

	private Lazy<NodeFieldTypeProvider> nodeFieldTypeProvider;

	@Inject
	public StringFieldTypeProvider(Lazy<NodeFieldTypeProvider> nodeFieldTypeProvider) {
		this.nodeFieldTypeProvider = nodeFieldTypeProvider;
	}

//	public GraphQLObjectType getStringFieldType() {
//		GraphQLObjectType stringFieldType = newObject().name("string").withInterface(nodeFieldTypeProvider.get().getFieldsType())
//				.field(newFieldDefinition().name("name").type(GraphQLString).build())
//				.field(newFieldDefinition().name("encoded").type(GraphQLBoolean).build()).build();
//		return stringFieldType;
//	}

}
