package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.PagingParameters;

import graphql.schema.GraphQLList;
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
		tagFamilyType.field(newFieldDefinition().name("name")
				.type(GraphQLString)
				.build());
		tagFamilyType.field(newFieldDefinition().name("tags")
				.argument(getPagingArgs())
				.type(new GraphQLList(new GraphQLTypeReference("Tag")))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof TagFamily) {

						InternalActionContext ac = ((InternalActionContext) ((Map) fetcher.getContext()).get("ac"));
						//TODO check for permission handling
						PagingParameters pagingInfo = getPagingParameters(fetcher);
						try {
							return ((TagFamily) source).getTags(ac.getUser(), pagingInfo);
						} catch (InvalidArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					return null;
				})
				.build());
		return tagFamilyType.build();
	}

}
