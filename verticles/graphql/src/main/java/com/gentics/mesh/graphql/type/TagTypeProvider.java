package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.relationship.GraphPermission;

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
					Tag tag = env.getSource();
					InternalActionContext ac = env.getContext();
					TagFamily tagFamily = tag.getTagFamily();
					if (ac.getUser()
							.hasPermission(tagFamily, GraphPermission.READ_PERM)) {
						return tagFamily;
					}
					return null;
				})
				.type(new GraphQLTypeReference("TagFamily")));

		// .nodes
		tagType.field(newPagingFieldWithFetcher("nodes", "Nodes which are tagged with the tag.", (env) -> {
			Tag tag = env.getSource();
			InternalActionContext ac = env.getContext();
			return tag.findTaggedNodes(ac.getUser(), ac.getRelease(), null, null, getPagingInfo(env));
		}, "Node"));

		return tagType.build();
	}
}
