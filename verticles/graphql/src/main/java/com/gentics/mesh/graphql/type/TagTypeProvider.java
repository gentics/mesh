package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Tag;

import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class TagTypeProvider extends AbstractTypeProvider {

	@Inject
	InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public TagTypeProvider() {
	}

	public GraphQLObjectType getTagType() {
		Builder tagType = newObject().name("Tag")
				.description("Tag of a node");
		interfaceTypeProvider.addCommonFields(tagType);

		// .name
		tagType.field(newFieldDefinition().name("name")
				.description("Name of the tag")
				.type(GraphQLString)
				.build());

		// .tagFamily
		tagType.field(newFieldDefinition().name("tagFamily")
				.description("Tag family to which the tag belongs")
				.type(new GraphQLTypeReference("TagFamily"))
				.build());

		// .nodes
		tagType.field(newFieldDefinition().name("nodes")
				.description("Nodes which are tagged with the tag")
				.argument(getPagingArgs())
				.type(new GraphQLList(new GraphQLTypeReference("Node")))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof Tag) {
						Tag tag = (Tag) source;
						InternalActionContext ac = (InternalActionContext) fetcher.getContext();
						return tag.getNodes(ac.getRelease());
					}
					return null;
				})
				.build());
		return tagType.build();
	}
}
