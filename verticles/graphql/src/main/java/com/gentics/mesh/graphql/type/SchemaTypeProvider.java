package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.ProjectReferenceTypeProvider.PROJECT_REFERENCE_PAGE_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.json.JsonUtil;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class SchemaTypeProvider extends AbstractTypeProvider {

	private static final Logger log = LoggerFactory.getLogger(SchemaTypeProvider.class);

	public static final String SCHEMA_TYPE_NAME = "Schema";

	public static final String SCHEMA_PAGE_TYPE_NAME = "SchemasPage";

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public SchemaTypeProvider() {
	}

	public GraphQLObjectType createType() {
		Builder schemaType = newObject().name(SCHEMA_TYPE_NAME).description("Node schema");
		interfaceTypeProvider.addCommonFields(schemaType);

		schemaType.field(newFieldDefinition().name("name").type(GraphQLString).dataFetcher((env) -> {
			Object source = env.getSource();
			if (source instanceof NamedElement) {
				return ((NamedElement) source).getName();
			}
			return null;
		}));

//		schemaType.field(newPagingFieldWithFetcher("projects", "Projects that this schema is assigned to", (env) -> {
//			GraphQLContext gc = env.getContext();
//			SchemaContainer schema = env.getSource();
//			return schema.findReferencedReleases().keySet().stream().map(Release::getProject).distinct()
//					.filter(it -> gc.getUser().hasPermission(it, GraphPermission.READ_PERM)).collect(Collectors.toList());
//		}, PROJECT_REFERENCE_PAGE_TYPE_NAME));

		schemaType.field(newFieldDefinition().name("isContainer").type(GraphQLBoolean).dataFetcher((env) -> {
			SchemaModel model = loadModelWithFallback(env);
			return model != null ? model.isContainer() : null;
		}));

		schemaType.field(newFieldDefinition().name("displayField").type(GraphQLString).dataFetcher((env) -> {
			SchemaModel model = loadModelWithFallback(env);
			return model != null ? model.getDisplayField() : null;
		}));

		schemaType.field(newFieldDefinition().name("segmentField").type(GraphQLString).dataFetcher((env) -> {
			SchemaModel model = loadModelWithFallback(env);
			return model != null ? model.getSegmentField() : null;
		}));

		// TODO add fields

		return schemaType.build();
	}

	private SchemaModel loadModelWithFallback(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof SchemaContainer) {
			SchemaContainer schema = env.getSource();
			SchemaModel model = JsonUtil.readValue(schema.getLatestVersion().getJson(), SchemaModelImpl.class);
			return model;
		}
		if (source instanceof SchemaContainerVersion) {
			SchemaContainerVersion schema = env.getSource();
			SchemaModel model = JsonUtil.readValue(schema.getJson(), SchemaModelImpl.class);
			return model;
		}
		log.error("Invalid type {" + source + "}.");
		return null;
	}
}
