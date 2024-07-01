package com.gentics.mesh.graphql.type.field;

import static com.gentics.mesh.graphql.type.NodeTypeProvider.nodeTypeName;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLUnionType.newUnionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.type.AbstractTypeProvider;
import com.gentics.mesh.graphql.type.MicroschemaTypeProvider;
import com.gentics.mesh.handler.Versioned;

import dagger.Lazy;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class MicronodeFieldTypeProvider extends AbstractTypeProvider {

	public static final String MICROSCHEMA_FIELD_TYPES = "microschemaFieldTypes";
	public static final String MICROSCHEMAS = "microschemasCache";
	public static final String MICRONODE_TYPE_NAME = "Micronode";

	private static final Logger log = LoggerFactory.getLogger(MicronodeFieldTypeProvider.class);

	protected final Lazy<FieldDefinitionProvider> fields;

	@Inject
	public MicronodeFieldTypeProvider(MeshOptions options, Lazy<FieldDefinitionProvider> fields) {
		super(options);
		this.fields = fields;
	}

	public Versioned<Optional<GraphQLType>> createType(GraphQLContext context) {
		return Versioned.<Optional<GraphQLType>>
		since(1, () -> {
			List<GraphQLObjectType> types = context.getOrStore(MICROSCHEMA_FIELD_TYPES, () -> generateMicroschemaFieldTypes(context).forVersion(context));
			return Optional.ofNullable(types).filter(CollectionUtils::isNotEmpty).map(list -> {
				GraphQLObjectType[] typeArray = list.toArray(new GraphQLObjectType[0]);

				GraphQLUnionType fieldType = newUnionType().name(MICRONODE_TYPE_NAME).possibleTypes(typeArray).description("Fields of the micronode.")
					.typeResolver(env -> {
						Object object = env.getObject();
						if (object instanceof HibMicronode) {
							HibMicronode fieldContainer = (HibMicronode) object;
							HibMicroschemaVersion micronodeFieldSchema = fieldContainer.getSchemaContainerVersion();
							String schemaName = micronodeFieldSchema.getName();
							GraphQLObjectType foundType = env.getSchema().getObjectType(schemaName);
							return foundType;
						}
						return null;
					}).build();

				return fieldType;
			});
		}).since(2, () -> {
			GraphQLInterfaceType fieldType = newInterface()
				.name(MICRONODE_TYPE_NAME)
				.typeResolver(env -> {
					HibMicronode fieldContainer = env.getObject();
					HibMicroschemaVersion micronodeFieldSchema = fieldContainer.getSchemaContainerVersion();
					String schemaName = micronodeFieldSchema.getName();
					return env.getSchema().getObjectType(schemaName);
				})
				.fields(createMicronodeFields())
				.build();

			return Optional.of(fieldType);
		}).build();
	}

	private List<GraphQLFieldDefinition> createMicronodeFields() {
		return Arrays.asList(
			newFieldDefinition()
				.name("uuid")
				.description("The uuid of the micronode")
				.type(GraphQLString)
				.dataFetcher(micronodeFetcher(HibElement::getUuid))
				.build(),
			newFieldDefinition()
				.name("microschema")
				.description("The microschema of this micronode")
				.type(GraphQLTypeReference.typeRef(MicroschemaTypeProvider.MICROSCHEMA_TYPE_NAME))
				.dataFetcher(micronodeFetcher(micronode -> micronode.getSchemaContainerVersion().getSchemaContainer()))
				.build()
		);
	}

	private DataFetcher<?> micronodeFetcher(Function<HibMicronode, ?> mapper) {
		return env -> mapper.apply(env.getSource());
	}

	public Versioned<List<GraphQLObjectType>> generateMicroschemaFieldTypes(GraphQLContext context) {
		return Versioned
		.since(1, () -> {
			Tx tx = Tx.get();
			Consumer<GraphQLFieldDefinition.Builder> addDeprecation = builder ->
				builder.deprecate("Usage of fields in micronodes has changed in /api/v2. See https://github.com/gentics/mesh/issues/317");
			HibProject project = tx.getProject(context);

			MicroschemaDao microschemaDao = Tx.get().microschemaDao();
			List<GraphQLObjectType> schemaTypes = new ArrayList<>();
			Result<? extends HibMicroschema> microschemas = context.getOrStore(MICROSCHEMAS + project.getName(), () -> microschemaDao.findAll(project));
			for (HibMicroschema container : microschemas) {
				HibMicroschemaVersion version = container.getLatestVersion();
				MicroschemaModel microschemaModel = version.getSchema();
				Builder microschemaType = newObject();
				microschemaType.name(microschemaModel.getName());
				microschemaType.description(microschemaModel.getDescription());

				for (FieldSchema fieldSchema : microschemaModel.getFields()) {
					FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
					switch (type) {
						case STRING:
							microschemaType.field(fields.get().createStringDef(fieldSchema).transform(addDeprecation));
							break;
						case HTML:
							microschemaType.field(fields.get().createHtmlDef(fieldSchema).transform(addDeprecation));
							break;
						case NUMBER:
							microschemaType.field(fields.get().createNumberDef(fieldSchema).transform(addDeprecation));
							break;
						case DATE:
							microschemaType.field(fields.get().createDateDef(fieldSchema).transform(addDeprecation));
							break;
						case BOOLEAN:
							microschemaType.field(fields.get().createBooleanDef(fieldSchema).transform(addDeprecation));
							break;
						case NODE:
							microschemaType.field(fields.get().createNodeDef(fieldSchema).transform(addDeprecation));
							break;
						case LIST:
							ListFieldSchema listFieldSchema = ((ListFieldSchema) fieldSchema);
							fields.get().createListDef(context, listFieldSchema, project).ifPresent(field -> microschemaType.field(field.transform(addDeprecation)));
							break;
						default:
							log.error("Micronode field type {" + type + "} is not supported.");
							// TODO throw exception for unsupported type
							break;
					}
				}
				GraphQLObjectType type = microschemaType.build();
				schemaTypes.add(type);
			}
			return schemaTypes;
		}).since(2, () -> {
			Tx tx = Tx.get();
			HibProject project = tx.getProject(context);
			Result<? extends HibMicroschema> microschemas = context.getOrStore(MICROSCHEMAS + project.getName(), () -> tx.microschemaDao().findAll(project));
			return microschemas.stream().map(container -> {
				HibMicroschemaVersion version = container.getLatestVersion();
				MicroschemaModel microschemaModel = version.getSchema();
				String microschemaName = microschemaModel.getName();

				Builder microschemaType = newObject();

				microschemaType.withInterface(GraphQLTypeReference.typeRef(MICRONODE_TYPE_NAME));
				microschemaType.name(microschemaName);
				microschemaType.description(microschemaModel.getDescription());

				if (!microschemaModel.getFields().isEmpty()) {
					GraphQLFieldDefinition.Builder fieldsField = newFieldDefinition();
					Builder fieldsType = newObject();
					fieldsType.name(nodeTypeName(microschemaName));
					fieldsField.dataFetcher(micronodeFetcher(Function.identity()));

					for (FieldSchema fieldSchema : microschemaModel.getFields()) {
						FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
						switch (type) {
						case STRING:
							fieldsType.field(fields.get().createStringDef(fieldSchema));
							break;
						case HTML:
							fieldsType.field(fields.get().createHtmlDef(fieldSchema));
							break;
						case NUMBER:
							fieldsType.field(fields.get().createNumberDef(fieldSchema));
							break;
						case DATE:
							fieldsType.field(fields.get().createDateDef(fieldSchema));
							break;
						case BOOLEAN:
							fieldsType.field(fields.get().createBooleanDef(fieldSchema));
							break;
						case NODE:
							fieldsType.field(fields.get().createNodeDef(fieldSchema));
							break;
						case LIST:
							ListFieldSchema listFieldSchema = ((ListFieldSchema) fieldSchema);
							fields.get().createListDef(context, listFieldSchema, project).ifPresent(fieldsType::field);
							break;
						default:
							log.error("Micronode field type {" + type + "} is not supported.");
							// TODO throw exception for unsupported type
							break;
						}

					}
					fieldsField.name("fields").type(fieldsType);
					microschemaType.field(fieldsField);
				}
				microschemaType.fields(createMicronodeFields());
				return microschemaType.build();
			}).collect(Collectors.toList());
		}).build();
	}

}
