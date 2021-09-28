package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.TagTypeProvider.TAG_PAGE_TYPE_NAME;
import static com.gentics.mesh.graphql.type.TagTypeProvider.TAG_TYPE_NAME;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.parameter.PagingParameters;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

/**
 * GraphQL type provider for tag family.
 */
@Singleton
public class TagFamilyTypeProvider extends AbstractTypeProvider {

	public static final String TAG_FAMILY_TYPE_NAME = "TagFamily";

	public static final String TAG_FAMILY_PAGE_TYPE_NAME = "TagFamiliesPage";

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	private final TagDAOActions tagActions;

	@Inject
	public TagFamilyTypeProvider(MeshOptions options, TagDAOActions tagActions) {
		super(options);
		this.tagActions = tagActions;
	}

	public GraphQLObjectType createType() {
		Builder tagFamilyType = newObject().name(TAG_FAMILY_TYPE_NAME);
		interfaceTypeProvider.addCommonFields(tagFamilyType);

		// .name
		tagFamilyType.field(newFieldDefinition().name("name").type(GraphQLString).dataFetcher((env) -> {
			HibTagFamily tagFamily = env.getSource();
			return tagFamily.getName();
		}));

		// .tag
		tagFamilyType.field(
			newFieldDefinition().name("tag").description("Load a specific tag by name or uuid.").argument(createUuidArg("Uuid of the tag."))
				.argument(createNameArg("Name of the tag.")).type(new GraphQLTypeReference(TAG_TYPE_NAME)).dataFetcher(env -> {
					HibTagFamily tagFamily = env.getSource();
					return handleUuidNameArgs(env, tagFamily, tagActions);
				}).build());

		// .tags
		tagFamilyType.field(newPagingFieldWithFetcher("tags", "Tags which are assigned to the tagfamily.", (env) -> {
			GraphQLContext gc = env.getContext();
			HibTagFamily tagFamily = env.getSource();
			PagingParameters pagingInfo = getPagingInfo(env);
			TagFamilyDao tagFamilyDao = Tx.get().tagFamilyDao();

			return tagFamilyDao.getTags(tagFamily, gc.getUser(), pagingInfo);
		}, TAG_PAGE_TYPE_NAME));
		return tagFamilyType.build();
	}

}
