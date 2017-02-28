package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class TagTypeProvider extends AbstractTypeProvider{

	@Inject
	InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public TagTypeProvider() {
	}

	public GraphQLObjectType getTagType() {
		Builder tagType = newObject().name("Tag");
		interfaceTypeProvider.addCommonFields(tagType);
		tagType.field(newFieldDefinition().name("name").type(GraphQLString).build());
		tagType.field(newFieldDefinition().name("tagFamily").type(new GraphQLTypeReference("TagFamily")).build());
		return tagType.build();
	}
}
