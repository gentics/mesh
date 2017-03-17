package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.graphdb.model.MeshElement;

import dagger.Lazy;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLInterfaceType.Builder;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class InterfaceTypeProvider extends AbstractTypeProvider {

	@Inject
	public Lazy<UserTypeProvider> userTypeProvider;

	@Inject
	public InterfaceTypeProvider() {
	}

	private GraphQLInterfaceType getCommonType() {

		Builder common = newInterface().name("MeshElement");
		// .uuid
		common.field(newFieldDefinition().name("uuid")
				.description("UUID of the element")
				.type(GraphQLString)
				.build());

		// .edited
		common.field(newFieldDefinition().name("edited")
				.description("ISO8601 formatted edit timestamp")
				.type(GraphQLString)
				.build());

		// .created
		common.field(newFieldDefinition().name("created")
				.description("ISO8601 formatted created date string")
				.type(GraphQLString)
				.build());

		// .permissions
		common.field(newFieldDefinition().name("permissions")
				.description("Permission information of the element")
				.type(getPermInfoType())
				.build());

		//TODO add rolePerms

		// .creator
		common.field(newFieldDefinition().name("creator")
				.description("Creator of the element")
				.type(new GraphQLTypeReference("User"))
				.build());

		// .editor
		common.field(newFieldDefinition().name("editor")
				.description("Editor of the element")
				.type(new GraphQLTypeReference("User"))
				.build());

		common.typeResolver(resolver -> {
			return null;
		});
		return common.build();
	}

	private GraphQLObjectType getPermInfoType() {
		graphql.schema.GraphQLObjectType.Builder builder = newObject().name("permissions")
				.description("Permission information");
		builder.field(newFieldDefinition().name("create")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the create permission is granted.")
				.build());
		builder.field(newFieldDefinition().name("read")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the read permission is granted.")
				.build());
		builder.field(newFieldDefinition().name("update")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the update permission is granted.")
				.build());
		builder.field(newFieldDefinition().name("delete")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the delete permission is granted.")
				.build());
		builder.field(newFieldDefinition().name("publish")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the publish permission is granted.")
				.build());
		builder.field(newFieldDefinition().name("readPublished")
				.type(GraphQLBoolean)
				.description("Flag which idicates whether the read published permission is granted.")
				.build());
		return builder.build();
	}

	public void addCommonFields(graphql.schema.GraphQLObjectType.Builder builder) {
		builder.withInterface(getCommonType());

		// .uuid
		builder.field(newFieldDefinition().name("uuid")
				.description("UUID of the element")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof MeshElement) {
						return ((MeshElement) source).getUuid();
					}
					return null;
				})
				.build());

		// .etag
		builder.field(newFieldDefinition().name("etag")
				.description("ETag of the element")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof TransformableElement) {
						InternalActionContext ac = (InternalActionContext) fetcher.getContext();
						return ((TransformableElement<?>) source).getETag(ac);
					}
					return null;
				})
				.build());

		// .permission
		builder.field(newFieldDefinition().name("permissions")
				.description("Permission information of the element")
				.type(getPermInfoType())
				.build());

		//TODO rolePerms

		// .edited
		builder.field(newFieldDefinition().name("edited")
				.description("ISO8601 formatted edit timestamp")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof EditorTrackingVertex) {
						return ((EditorTrackingVertex) source).getLastEditedDate();
					}
					return null;
				})
				.build());

		// .created
		builder.field(newFieldDefinition().name("created")
				.description("ISO8601 formatted created date string")
				.type(GraphQLString)
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof CreatorTrackingVertex) {
						return ((CreatorTrackingVertex) source).getCreationDate();
					}
					return null;
				})
				.build());

		// .creator
		builder.field(newFieldDefinition().name("creator")
				.description("Creator of the element")
				.type(new GraphQLTypeReference("User"))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof CreatorTrackingVertex) {
						return ((CreatorTrackingVertex) source).getCreator();
					}
					return null;
				})
				.build());

		// .editor
		builder.field(newFieldDefinition().name("editor")
				.description("Editor of the element")
				.type(new GraphQLTypeReference("User"))
				.dataFetcher(fetcher -> {
					Object source = fetcher.getSource();
					if (source instanceof EditorTrackingVertex) {
						return ((EditorTrackingVertex) source).getEditor();
					}
					return null;
				})
				.build());

	}

}
