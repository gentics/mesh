package com.gentics.mesh.graphql.type.field;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.type.AbstractTypeProvider;
import dagger.Lazy;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLUnionType;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLUnionType.newUnionType;

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
		Map<String, GraphQLObjectType> types = generateMicroschemaFieldType(context);
		// No microschemas have been found - We need to add a dummy type in order to keep the type system working
		if (types.isEmpty()) {
			types.put("dummy", newObject().name("dummy").field(newFieldDefinition().name("dummy").type(GraphQLString).staticValue(null).build()).description("Placeholder dummy microschema type").build());
		}
		GraphQLObjectType[] typeArray = types.values().toArray(new GraphQLObjectType[types.values().size()]);

		GraphQLUnionType fieldType = newUnionType().name(MICRONODE_TYPE_NAME).possibleTypes(typeArray).description("Fields of the micronode.")
				.typeResolver(env -> {
					Object object = env.getObject();
					if (object instanceof Micronode) {
						Micronode fieldContainer = (Micronode) object;
						MicroschemaContainerVersion micronodeFieldSchema = fieldContainer.getSchemaContainerVersion();
						String schemaName = micronodeFieldSchema.getName();
						GraphQLObjectType foundType = types.get(schemaName);
						return foundType;
					}
					return null;
				}).build();

		return fieldType;
	}

	private Map<String, GraphQLObjectType> generateMicroschemaFieldType(GraphQLContext context) {
		Project project = context.getProject();
		Map<String, GraphQLObjectType> schemaTypes = new HashMap<>();
		for (MicroschemaContainer container : project.getMicroschemaContainerRoot().findAll()) {
			MicroschemaContainerVersion version = container.getLatestVersion();
			Microschema microschema = version.getSchema();
			Builder microschemaType = newObject();
			microschemaType.name(microschema.getName());
			microschemaType.description(microschema.getDescription());

			for (FieldSchema fieldSchema : microschema.getFields()) {
				FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
				switch (type) {
				case STRING:
					microschemaType.field(fields.get().createStringDef(fieldSchema));
					break;
				case HTML:
					microschemaType.field(fields.get().createHtmlDef(fieldSchema));
					break;
				case NUMBER:
					microschemaType.field(fields.get().createNumberDef(fieldSchema));
					break;
				case DATE:
					microschemaType.field(fields.get().createDateDef(fieldSchema));
					break;
				case BOOLEAN:
					microschemaType.field(fields.get().createBooleanDef(fieldSchema));
					break;
				case NODE:
					microschemaType.field(fields.get().createNodeDef(fieldSchema));
					break;
				case LIST:
					ListFieldSchema listFieldSchema = ((ListFieldSchema) fieldSchema);
					microschemaType.field(fields.get().createListDef(context, listFieldSchema));
					break;
				default:
					log.error("Micronode field type {" + type + "} is not supported.");
					// TODO throw exception for unsupported type
					break;
				}

			}
			GraphQLObjectType type = microschemaType.build();
			schemaTypes.put(microschema.getName(), type);
		}
		return schemaTypes;
	}

}
