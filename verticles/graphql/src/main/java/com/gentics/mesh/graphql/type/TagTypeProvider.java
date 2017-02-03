package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;

@Singleton
public class TagTypeProvider {

	@Inject
	public TagTypeProvider() {
	}

	public GraphQLObjectType getTagType() {
		Builder tagType = newObject().name("tag");
		tagType.field(newFieldDefinition().name("name").type(GraphQLString).build());
		tagType.field(newFieldDefinition().name("uuid").type(GraphQLString).build());
		return tagType.build();
	}
}
