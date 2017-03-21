package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.parameter.PagingParameters;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class TagFamilyTypeProvider extends AbstractTypeProvider {

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public TagFamilyTypeProvider() {
	}

	public GraphQLObjectType getTagFamilyType() {
		Builder tagFamilyType = newObject().name("TagFamily");
		interfaceTypeProvider.addCommonFields(tagFamilyType);

		// .name
		tagFamilyType.field(newFieldDefinition().name("name")
				.type(GraphQLString));

		// .tag
		tagFamilyType.field(newFieldDefinition().name("tag")
				.description("Load a specific tag by name or uuid.")
				.argument(getUuidArg("Uuid of the tag."))
				.argument(getNameArg("Name of the tag."))
				.type(new GraphQLTypeReference("Tag"))
				.dataFetcher(env -> {
					TagFamily tagFamily = env.getSource();
					return handleUuidNameArgs(env, tagFamily);
				})
				.build());

		// .tags
		tagFamilyType.field(newPagingFieldWithFetcher("tags", "Tags which are assigned to the tagfamily.", (env) -> {
			TagFamily tagFamily = env.getSource();
			InternalActionContext ac = env.getContext();
			PagingParameters pagingInfo = getPagingParameters(env);
			return tagFamily.getTags(ac.getUser(), pagingInfo);
		}, "Tag"));
		return tagFamilyType.build();
	}

}
