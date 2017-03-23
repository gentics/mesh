package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.graphql.context.GraphQLContext;

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

	/**
	 * Create the tag type.
	 * 
	 * @return
	 */
	public GraphQLObjectType createTagType() {
		Builder tagType = newObject().name("Tag")
				.description("Tag of a node.");
		interfaceTypeProvider.addCommonFields(tagType);

		// .name
		tagType.field(newFieldDefinition().name("name")
				.description("Name of the tag")
				.type(GraphQLString));

		// .tagFamily
		tagType.field(newFieldDefinition().name("tagFamily")
				.description("Tag family to which the tag belongs")
				.dataFetcher((env) -> {
					GraphQLContext gc = env.getContext();
					Tag tag = env.getSource();
					TagFamily tagFamily = tag.getTagFamily();
					return gc.requiresPerm(tagFamily, READ_PERM);
				})
				.type(new GraphQLTypeReference("TagFamily")));

		// .nodes
		tagType.field(newPagingFieldWithFetcher("nodes", "Nodes which are tagged with the tag.", (env) -> {
			GraphQLContext gc = env.getContext();
			Tag tag = env.getSource();
			return tag.findTaggedNodes(gc.getUser(), gc.getRelease(), null, null, getPagingInfo(env));
		}, "Node"));

		return tagType.build();
	}
}
