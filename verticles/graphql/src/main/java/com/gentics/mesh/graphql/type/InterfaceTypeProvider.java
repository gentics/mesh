package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.data.user.HibEditorTracking;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;

import dagger.Lazy;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;

@Singleton
public class InterfaceTypeProvider extends AbstractTypeProvider {

	public static final String PERM_INFO_TYPE_NAME = "PermInfo";

	@Inject
	public Lazy<UserTypeProvider> userTypeProvider;

	@Inject
	public InterfaceTypeProvider(MeshOptions options) {
		super(options);
	}

	// protected GraphQLInterfaceType createCommonType() {
	//
	// Builder common = newInterface().name("MeshElement");
	// // .uuid
	// common.field(newFieldDefinition().name("uuid").description("UUID of the element").type(GraphQLString));
	//
	// // .edited
	// common.field(newFieldDefinition().name("edited").description("ISO8601 formatted edit timestamp").type(GraphQLString));
	//
	// // .created
	// common.field(newFieldDefinition().name("created").description("ISO8601 formatted created date string").type(GraphQLString));
	//
	// // .permissions
	// common.field(newFieldDefinition().name("permissions").description("Permission information of the element").type(createPermInfoType()));
	//
	// // TODO add rolePerms
	//
	// // .creator
	// common.field(newFieldDefinition().name("creator").description("Creator of the element").type(new GraphQLTypeReference("User")));
	//
	// // .editor
	// common.field(newFieldDefinition().name("editor").description("Editor of the element").type(new GraphQLTypeReference("User")));
	//
	// common.typeResolver(env -> {
	// return null;
	// });
	// return common.build();
	// }

	/**
	 * Create the permission information type.
	 * 
	 * @return
	 */
	public GraphQLObjectType createPermInfoType() {
		graphql.schema.GraphQLObjectType.Builder builder = newObject().name(PERM_INFO_TYPE_NAME).description("Permission information");

		// .create
		builder.field(newFieldDefinition().name("create").type(GraphQLBoolean)
				.description("Flag which indicates whether the create permission is granted."));

		// .read
		builder.field(
				newFieldDefinition().name("read").type(GraphQLBoolean).description("Flag which indicates whether the read permission is granted."));

		// .update
		builder.field(newFieldDefinition().name("update").type(GraphQLBoolean)
				.description("Flag which indicates whether the update permission is granted."));

		// .delete
		builder.field(newFieldDefinition().name("delete").type(GraphQLBoolean)
				.description("Flag which indicates whether the delete permission is granted."));

		// .publish
		builder.field(newFieldDefinition().name("publish").type(GraphQLBoolean)
				.description("Flag which indicates whether the publish permission is granted."));

		// .readPublished
		builder.field(newFieldDefinition().name("readPublished").type(GraphQLBoolean)
				.description("Flag which indicates whether the read published permission is granted."));
		return builder.build();
	}

	public void addCommonFields(graphql.schema.GraphQLObjectType.Builder builder) {
		addCommonFields(builder, false);
	}

	/**
	 * Add common fields to the given builder.
	 * 
	 * @param builder
	 * @param isNode
	 *            Flag which indicates whether the builder is for a node. Nodes do not require certain common fields. Those fields will be excluded.
	 */
	public void addCommonFields(graphql.schema.GraphQLObjectType.Builder builder, boolean isNode) {
		setFields(builder::field, isNode);
	}

	public void addCommonFields(graphql.schema.GraphQLInterfaceType.Builder builder) {
		addCommonFields(builder, false);
	}

	/**
	 * Add common fields to the given builder.
	 *
	 * @param builder
	 * @param isNode
	 *            Flag which indicates whether the builder is for a node. Nodes do not require certain common fields. Those fields will be excluded.
	 */
	public void addCommonFields(graphql.schema.GraphQLInterfaceType.Builder builder, boolean isNode) {
		setFields(builder::field, isNode);
	}

	private void setFields(Consumer<GraphQLFieldDefinition.Builder> setField, boolean isNode) {
		// builder.withInterface(createCommonType());

		// .uuid
		setField.accept(newFieldDefinition().name("uuid").description("UUID of the element").type(GraphQLString).dataFetcher(env -> {
			HibBaseElement element = null;
			Object source = env.getSource();
			if (source instanceof NodeContent) {
				element = ((NodeContent) source).getNode();
			} else if (source instanceof SchemaVersion) {
				element = ((SchemaVersion) source).getSchemaContainer();
			} else {
				element = env.getSource();
			}
			return element.getUuid();
		}));

		// .etag
		setField.accept(newFieldDefinition().name("etag").description("ETag of the element").type(GraphQLString).dataFetcher(env -> {
			GraphQLContext gc = env.getContext();
			TransformableElement<?> element = env.getSource();
			return element.getETag(gc);
		}));

		// .permission
		setField.accept(newFieldDefinition().name("permissions").description("Permission information of the element")
			.type(new GraphQLTypeReference(PERM_INFO_TYPE_NAME))
			.dataFetcher(env -> {
				GraphQLContext gc = env.getContext();
				HibCoreElement element = getMeshCoreElement(env.getSource());

				UserDaoWrapper userDao = Tx.get().userDao();
				return userDao.getPermissionInfo(gc.getUser(), element);
			})
		);

		setField.accept(newFieldDefinition().name("rolePerms").description("Permissions information of the element for a certain role")
			.type(new GraphQLTypeReference(PERM_INFO_TYPE_NAME))
			.argument(new GraphQLArgument.Builder()
				.name("role")
				.description("The uuid of the role to get permissions for")
				.type(nonNull(GraphQLString))
			)
			.dataFetcher(env -> {
				GraphQLContext gc = env.getContext();
				HibCoreElement element = getMeshCoreElement(env.getSource());

				UserDaoWrapper userDao = Tx.get().userDao();
				return userDao.getRolePermissions(element, gc, env.getArgument("role"));
			})
		);

		// .created
		setField.accept(
				newFieldDefinition().name("created").description("ISO8601 formatted created date string").type(GraphQLString).dataFetcher(env -> {
					// The source element might be a NGFC. These containers have no creator. The creator is stored for it's Node instead
					Object source = env.getSource();
					HibCreatorTracking element = null;
					if (source instanceof NodeContent) {
						element = ((NodeContent) source).getNode();
					} else if (source instanceof SchemaVersion) {
						element = ((HibSchemaVersion) source).getSchemaContainer();
					} else {
						element = env.getSource();
					}
					return element.getCreationDate();
				}));

		// .creator
		setField.accept(
				newFieldDefinition().name("creator").description("Creator of the element").type(new GraphQLTypeReference("User")).dataFetcher(env -> {
					GraphQLContext gc = env.getContext();
					// The source element might be a NGFC. These containers have no creator. The creator is stored for it's Node instead
					Object source = env.getSource();
					HibCreatorTracking element = null;
					if (source instanceof NodeContent) {
						element = ((NodeContent) source).getNode();
					} else if (source instanceof HibSchemaVersion) {
						element = ((HibSchemaVersion) source).getSchemaContainer();
					} else {
						element = env.getSource();
					}
					return gc.requiresPerm(element.getCreator(), READ_PERM);
				}));
		if (!isNode) {
			// .edited
			setField.accept(newFieldDefinition().name("edited").description("ISO8601 formatted edit timestamp").type(GraphQLString).dataFetcher(env -> {
				Object source = env.getSource();
				if (source instanceof HibSchemaVersion) {
					source = ((SchemaVersion) source).getSchemaContainer();
				}
				if (source instanceof HibEditorTracking) {
					HibEditorTracking vertex = (HibEditorTracking) source;
					return vertex.getLastEditedDate();
				}
				return null;
			}));

			// .editor
			setField.accept(newFieldDefinition().name("editor").description("Editor of the element").type(new GraphQLTypeReference("User"))
					.dataFetcher(env -> {
						Object source = env.getSource();
						if (source instanceof SchemaVersion) {
							source = ((SchemaVersion) source).getSchemaContainer();
						}
						if (source instanceof EditorTrackingVertex) {
							GraphQLContext gc = env.getContext();
							EditorTrackingVertex vertex = (EditorTrackingVertex) source;
							return gc.requiresPerm(vertex.getEditor(), READ_PERM);
						}
						return null;
					}));
		}
	}

	private HibCoreElement getMeshCoreElement(Object source) {
		if (source instanceof NodeContent) {
			return ((NodeContent) source).getNode();
		} else if (source instanceof SchemaVersion) {
			return ((SchemaVersion) source).getSchemaContainer();
		} else {
			return (HibCoreElement) source;
		}
	}

}
