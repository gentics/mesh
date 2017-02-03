package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class TagFamilyTypeProvider {

	@Inject
	public TagFamilyTypeProvider() {
	}
	
	public GraphQLObjectType getTagFamilyType() {
		Builder tagFamilyType = newObject().name("tagFamily");
		tagFamilyType.field(newFieldDefinition().name("name").type(GraphQLString).build());
		tagFamilyType.field(newFieldDefinition().name("uuid").type(GraphQLString).build());
		return tagFamilyType.build();
	}
}
