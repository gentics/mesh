package com.gentics.mesh.graphql.type;

import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLUnionType.newUnionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.graphql.type.field.FieldDefinitionProvider;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLUnionType;

/**
 * This class contains all field specific type code. It provides methods which can be used to generate field types for a specific project. This is done by
 * examining the schemas which are assigned to the project release.
 */
@Singleton
public class NodeFieldTypeProvider extends AbstractTypeProvider {

	public static final String NODE_FIELDS_TYPE_NAME = "Fields";

	@Inject
	public FieldDefinitionProvider fields;

	@Inject
	public NodeFieldTypeProvider() {
	}

	public GraphQLUnionType getSchemaFieldsType(Project project) {
		Map<String, GraphQLObjectType> types = generateSchemaFieldType(project);

		GraphQLObjectType[] typeArray = types.values().toArray(new GraphQLObjectType[types.values().size()]);

		GraphQLUnionType fieldType = newUnionType().name(NODE_FIELDS_TYPE_NAME).possibleTypes(typeArray).description("Fields of the node.")
				.typeResolver(env -> {
					Object object = env.getObject();
					if (object instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer fieldContainer = (NodeGraphFieldContainer) object;
						return types.get(fieldContainer.getSchemaContainerVersion().getName());
					}
					return null;
				}).build();

		return fieldType;

	}

	/**
	 * Generate a map of all schema types which correspond to schemas which are part of the project/release.
	 * 
	 * @param project
	 * @return
	 */
	private Map<String, GraphQLObjectType> generateSchemaFieldType(Project project) {
		Map<String, GraphQLObjectType> schemaTypes = new HashMap<>();
		List<GraphQLObjectType> list = new ArrayList<>();
		for (SchemaContainer container : project.getSchemaContainerRoot().findAll()) {
			SchemaContainerVersion version = container.getLatestVersion();
			Schema schema = version.getSchema();
			Builder root = newObject();
			// TODO remove this workaround
			root.name(schema.getName().replaceAll("-", "_"));
			root.description(schema.getDescription());

			// TODO add link resolving argument / code
			for (FieldSchema fieldSchema : schema.getFields()) {
				FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
				switch (type) {
				case STRING:
					root.field(fields.createStringDef(fieldSchema));
					break;
				case HTML:
					root.field(fields.createHtmlDef(fieldSchema));
					break;
				case NUMBER:
					root.field(fields.createNumberDef(fieldSchema));
					break;
				case DATE:
					root.field(fields.createDateDef(fieldSchema));
					break;
				case BOOLEAN:
					root.field(fields.createBooleanDef(fieldSchema));
					break;
				case NODE:
					root.field(fields.createNodeDef(fieldSchema));
					break;
				case BINARY:
					root.field(fields.createBinaryDef(fieldSchema));
					break;
				case LIST:
					ListFieldSchema listFieldSchema = ((ListFieldSchema) fieldSchema);
					root.field(fields.createListDef(listFieldSchema));
					break;
				case MICRONODE:
					root.field(fields.createMicronodeDef(fieldSchema, project));
					break;
				}
			}
			GraphQLObjectType type = root.build();
			list.add(type);
			schemaTypes.put(schema.getName(), type);
		}
		return schemaTypes;
	}

}
