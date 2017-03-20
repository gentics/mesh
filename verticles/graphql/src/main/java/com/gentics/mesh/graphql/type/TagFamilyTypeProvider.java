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

		// .tags
		tagFamilyType.field(newPagingFieldWithFetcher("tags", "Tags which are assigned to the tagfamily.", (env) -> {
			Object source = env.getSource();
			if (source instanceof TagFamily) {
				InternalActionContext ac = (InternalActionContext) env.getContext();
				PagingParameters pagingInfo = getPagingParameters(env);
				TagFamily tagFamily = (TagFamily) source;
				return tagFamily.getTags(ac.getUser(), pagingInfo);
			}
			return null;
		}, "Tag"));
		return tagFamilyType.build();
	}

}
