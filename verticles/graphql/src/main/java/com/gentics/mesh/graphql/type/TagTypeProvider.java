package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.TagFamilyTypeProvider.TAG_FAMILY_TYPE_NAME;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class TagTypeProvider extends AbstractTypeProvider {

	public static final String TAG_TYPE_NAME = "Tag";

	public static final String TAG_PAGE_TYPE_NAME = "TagsPage";

	private final InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public TagTypeProvider(MeshOptions options, InterfaceTypeProvider interfaceTypeProvider) {
		super(options);
		this.interfaceTypeProvider = interfaceTypeProvider;
	}

	/**
	 * Create the tag type.
	 * 
	 * @return
	 */
	public GraphQLObjectType createType() {
		Builder tagType = newObject().name(TAG_TYPE_NAME).description("Tag of a node.");
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
		}).type(new GraphQLTypeReference(TAG_FAMILY_TYPE_NAME)));

		// .nodes
		tagType.field(newFieldDefinition().name("nodes").description("Nodes which are tagged with the tag.")
				.type(new GraphQLTypeReference(NODE_PAGE_TYPE_NAME))
				.argument(createPagingArgs())
				.argument(createLanguageTagArg(true))
				.dataFetcher((env) -> {
					GraphQLContext gc = env.getContext();
					Tag tag = env.getSource();
					
					List<String> languageTags = getLanguageArgument(env);

					Stream<NodeContent> contents = tag.findTaggedNodes(gc, READ_PUBLISHED_PERM).stream()
						// Now lets try to load the containers for those found nodes - apply the language fallback
						.map(node -> new NodeContent(node, node.findVersion(gc, languageTags), languageTags))
						// Filter nodes without a container
						.filter(nodeContentFilter.forVersion(gc));

					return applyNodeFilter(env, contents);

				}));

		return tagType.build();
	}

}
