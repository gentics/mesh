package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.graphql.type.argument.ArgumentsProvider;

import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class TagFamilyTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public ArgumentsProvider argumentTypeProvider;
	
	@Inject
	public TagFamilyTypeProvider() {
	}

	public GraphQLObjectType getTagFamilyType() {
		Builder tagFamilyType = newObject().name("TagFamily");
		interfaceTypeProvider.addCommonFields(tagFamilyType);
		tagFamilyType.field(newFieldDefinition().name("name").type(GraphQLString).build());
		tagFamilyType.field(newFieldDefinition().name("tags").argument(argumentTypeProvider.getPagingArgs()).type(new GraphQLList(new GraphQLTypeReference("Tag"))).build());
		return tagFamilyType.build();
	}
}
