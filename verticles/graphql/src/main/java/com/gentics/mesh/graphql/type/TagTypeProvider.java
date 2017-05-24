package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.PageImpl;
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
		Builder tagType = newObject().name("Tag").description("Tag of a node.");
		interfaceTypeProvider.addCommonFields(tagType);

		// .name
		tagType.field(newFieldDefinition().name("name").description("Name of the tag").type(GraphQLString).dataFetcher((env) -> {
			Tag tag = env.getSource();
			return tag.getName();
		}));

		// .tagFamily
		tagType.field(newFieldDefinition().name("tagFamily").description("Tag family to which the tag belongs").dataFetcher((env) -> {
			GraphQLContext gc = env.getContext();
			Tag tag = env.getSource();
			TagFamily tagFamily = tag.getTagFamily();
			return gc.requiresPerm(tagFamily, READ_PERM);
		}).type(new GraphQLTypeReference("TagFamily")));

		// .nodes
		tagType.field(newFieldDefinition().name("nodes").description("Nodes which are tagged with the tag.")
				.type(newPageType("nodes", new GraphQLTypeReference("Node"))).argument(createPagingArgs()).argument(createLanguageTagArg())
				.dataFetcher((env) -> {
					GraphQLContext gc = env.getContext();
					Tag tag = env.getSource();
					TransformablePage<? extends Node> nodes = tag.findTaggedNodes(gc.getUser(), gc.getRelease(), null, null, getPagingInfo(env));
					List<String> languageTags = getLanguageArgument(env);

					// Transform the found nodes into contents
					List<NodeContent> contents = nodes.getWrappedList().stream().map(node -> {
						NodeGraphFieldContainer container = node.findNextMatchingFieldContainer(gc, languageTags);
						return new NodeContent(node, container);
					}).collect(Collectors.toList());
					return new PageImpl<NodeContent>(contents, nodes);
				}));

		return tagType.build();
	}
}
