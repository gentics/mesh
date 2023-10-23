package com.gentics.mesh.graphql.type;

import static com.gentics.mesh.graphql.type.ProjectReferenceTypeProvider.PROJECT_REFERENCE_PAGE_TYPE_NAME;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.filter.ProjectFilter;
import com.gentics.mesh.json.JsonUtil;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLOutputType;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * GraphQL type provider for microschema types.
 */
@Singleton
public class MicroschemaTypeProvider extends AbstractTypeProvider {

	private static final Logger log = LoggerFactory.getLogger(MicroschemaTypeProvider.class);

	public static final String MICROSCHEMA_TYPE_NAME = "Microschema";

	public static final String MICROSCHEMA_PAGE_TYPE_NAME = "MicroschemasPage";

	public static final String MICROSCHEMA_FIELD_TYPE = "MicroschemaFieldType";

	protected final InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public MicroschemaTypeProvider(MeshOptions options, InterfaceTypeProvider interfaceTypeProvider) {
		super(options);
		this.interfaceTypeProvider = interfaceTypeProvider;
	}

	/**
	 * Create the type definition.
	 * 
	 * @return
	 */
	public GraphQLObjectType createType() {
		Builder schemaType = newObject().name(MICROSCHEMA_TYPE_NAME).description("Microschema");
		interfaceTypeProvider.addCommonFields(schemaType);

		// .name
		schemaType.field(newFieldDefinition().name("name").description("Name of the microschema").type(GraphQLString));

		// .version
		schemaType.field(newFieldDefinition().name("version").description("Version of the microschema.").type(GraphQLInt));

		// .description
		schemaType.field(newFieldDefinition().name("description").description("Description of the microschema.").type(GraphQLString));

		// .projects
		schemaType.field(newPagingFieldWithFetcherBuilder("projects", "Load projects that this schema is attached to", env -> {
			GraphQLContext gc = env.getContext();
			HibMicroschema microschema = env.getSource();
			UserDao userDao = Tx.get().userDao();
			Stream<HibProject> projects = microschema.findReferencedBranches().keySet().stream()
					.map(HibBranch::getProject)
					.distinct()
					.filter(it -> userDao.hasPermission(gc.getUser(), it, InternalPermission.READ_PERM));

			return applyFilter(env, projects, ProjectFilter.filter());
		}, PROJECT_REFERENCE_PAGE_TYPE_NAME)
			.argument(ProjectFilter.filter().createFilterArgument()));

		// .isEmpty
		schemaType.field(newFieldDefinition().name("isEmpty").type(GraphQLBoolean).dataFetcher((env) -> {
			MicroschemaVersionModel model = loadModelWithFallback(env);
			return model == null || model.getFields() == null || model.getFields().isEmpty();
		}));

		// .fields
		Builder fieldListBuilder = newObject().name(MICROSCHEMA_FIELD_TYPE).description("List of schema fields");

		// .name
		fieldListBuilder.field(newFieldDefinition().name("name").type(GraphQLString).description("Name of the field"));

		// .label
		fieldListBuilder.field(newFieldDefinition().name("label").type(GraphQLString).description("Label of the field"));

		// .required
		fieldListBuilder.field(newFieldDefinition().name("required").type(GraphQLBoolean).description("Whether this field is required"));

		// .type
		fieldListBuilder.field(newFieldDefinition().name("type").type(GraphQLString).description("The type of the field"));
		// TODO add "allow" and "indexSettings"

		GraphQLOutputType type = GraphQLList.list(fieldListBuilder.build());

		// .fields
		schemaType.field(newFieldDefinition().name("fields").type(type).dataFetcher(env -> {
			List<FieldSchema> fields = loadModelWithFallback(env).getFields();
			if (fields.isEmpty()) {
				fields = Collections.singletonList(emptySchemaFieldDummy("This microschema has no fields. Do not use this filter."));
			}
			return fields;
		}));

		return schemaType.build();
	}

	private MicroschemaModelImpl loadModelWithFallback(DataFetchingEnvironment env) {
		Object source = env.getSource();
		if (source instanceof HibMicroschema) {
			HibMicroschema schema = env.getSource();
			MicroschemaModelImpl model = JsonUtil.readValue(schema.getLatestVersion().getJson(), MicroschemaModelImpl.class);
			return model;
		}
		if (source instanceof HibMicroschemaVersion) {
			HibMicroschemaVersion schema = env.getSource();
			MicroschemaModelImpl model = JsonUtil.readValue(schema.getJson(), MicroschemaModelImpl.class);
			return model;
		}
		log.error("Invalid type {" + source + "}.");
		return null;
	}
}
