package com.gentics.mesh.graphql.type.field;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.type.AbstractTypeProvider;
import com.gentics.mesh.graphql.type.MicroschemaTypeProvider;
import dagger.Lazy;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gentics.mesh.graphql.type.NodeTypeProvider.nodeTypeName;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;

@Singleton
public class MicronodeFieldTypeProvider extends AbstractTypeProvider {

	public static final String MICRONODE_TYPE_NAME = "Micronode";

	private static final Logger log = LoggerFactory.getLogger(MicronodeFieldTypeProvider.class);

	@Inject
	public Lazy<FieldDefinitionProvider> fields;

	@Inject
	public MicronodeFieldTypeProvider() {
	}

	public GraphQLType createType(GraphQLContext context) {
		GraphQLInterfaceType fieldType = newInterface()
			.name(MICRONODE_TYPE_NAME)
			.typeResolver(env -> {
				Micronode fieldContainer = env.getObject();
				MicroschemaContainerVersion micronodeFieldSchema = fieldContainer.getSchemaContainerVersion();
				String schemaName = micronodeFieldSchema.getName();
				return env.getSchema().getObjectType(schemaName);
			})
			.fields(createMicronodeFields())
			.build();

		return fieldType;
	}

	private List<GraphQLFieldDefinition> createMicronodeFields() {
		return Arrays.asList(
			newFieldDefinition()
				.name("uuid")
				.description("The uuid of the micronode")
				.type(GraphQLString)
				.dataFetcher(micronodeFetcher(MeshElement::getUuid))
				.build(),
			newFieldDefinition()
				.name("microschema")
				.description("The microschema of this micronode")
				.type(GraphQLTypeReference.typeRef(MicroschemaTypeProvider.MICROSCHEMA_TYPE_NAME))
				.dataFetcher(micronodeFetcher(micronode -> micronode.getSchemaContainerVersion().getSchemaContainer()))
				.build()
		);
	}

	private DataFetcher<?> micronodeFetcher(Function<Micronode, ?> mapper) {
		return env -> mapper.apply(env.getSource());
	}

	public List<GraphQLObjectType> generateMicroschemaFieldTypes(GraphQLContext context) {
		Project project = context.getProject();
		return project.getMicroschemaContainerRoot().findAll().stream().map(container -> {
			MicroschemaContainerVersion version = container.getLatestVersion();
			Microschema microschema = version.getSchema();
			String microschemaName = microschema.getName();

			Builder microschemaType = newObject();

			microschemaType.withInterface(GraphQLTypeReference.typeRef(MICRONODE_TYPE_NAME));
			microschemaType.name(microschemaName);
			microschemaType.description(microschema.getDescription());

			GraphQLFieldDefinition.Builder fieldsField = GraphQLFieldDefinition.newFieldDefinition();
			Builder fieldsType = newObject();
			fieldsType.name(nodeTypeName(microschemaName));
			fieldsField.dataFetcher(micronodeFetcher(Function.identity()));

			for (FieldSchema fieldSchema : microschema.getFields()) {
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
					fieldsType.field(fields.get().createListDef(context, listFieldSchema));
					break;
				default:
					log.error("Micronode field type {" + type + "} is not supported.");
					// TODO throw exception for unsupported type
					break;
				}

			}
			fieldsField.name("fields").type(fieldsType);
			microschemaType.field(fieldsField);
			microschemaType.fields(createMicronodeFields());
			return microschemaType.build();
		}).collect(Collectors.toList());
	}

}
