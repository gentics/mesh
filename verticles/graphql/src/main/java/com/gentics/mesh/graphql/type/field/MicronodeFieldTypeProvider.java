package com.gentics.mesh.graphql.type.field;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLFieldDefinition;

@Singleton
public class MicronodeFieldTypeProvider {

	@Inject
	public MicronodeFieldTypeProvider() {
	}

	public GraphQLFieldDefinition getFieldDefinition(String name, String label) {
		return null;
	}

}
