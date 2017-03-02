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

@Singleton
public class NodeFieldTypeProvider extends AbstractTypeProvider {

	@Inject
	public FieldDefinitionProvider fields;

	@Inject
	public NodeFieldTypeProvider() {
	}

	public GraphQLUnionType getSchemaFieldsType(Project project) {
		Map<String, GraphQLObjectType> types = generateSchemaFieldType(project);

		GraphQLObjectType[] typeArray = types.values()
				.toArray(new GraphQLObjectType[types.values()
						.size()]);

		GraphQLUnionType fieldType = newUnionType().name("Fields")
				.possibleTypes(typeArray)
				.description("Fields of the node.")
				.typeResolver(object -> {
					if (object instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer fieldContainer = (NodeGraphFieldContainer) object;
						return types.get(fieldContainer.getSchemaContainerVersion()
								.getName());
					}
					return null;
				})
				.build();

		return fieldType;

	}

	private Map<String, GraphQLObjectType> generateSchemaFieldType(Project project) {
		Map<String, GraphQLObjectType> schemaTypes = new HashMap<>();
		List<GraphQLObjectType> list = new ArrayList<>();
		for (SchemaContainer container : project.getSchemaContainerRoot()
				.findAll()) {
			SchemaContainerVersion version = container.getLatestVersion();
			Schema schema = version.getSchema();
			Builder root = newObject();
			root.name(schema.getName());
			root.description(schema.getDescription());

			for (FieldSchema fieldSchema : schema.getFields()) {
				FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
				switch (type) {
				case STRING:
					root.field(fields.getStringDef(fieldSchema));
					break;
				case HTML:
					root.field(fields.getHtmlDef(fieldSchema));
					break;
				case NUMBER:
					root.field(fields.getNumberDef(fieldSchema));
					break;
				case DATE:
					root.field(fields.getDateDef(fieldSchema));
					break;
				case BOOLEAN:
					root.field(fields.getBooleanDef(fieldSchema));
					break;
				case NODE:
					root.field(fields.getNodeDef(fieldSchema));
					break;
				case BINARY:
					root.field(fields.getBinaryDef(fieldSchema));
					break;
				case LIST:
					ListFieldSchema listFieldSchema = ((ListFieldSchema) fieldSchema);
					root.field(fields.getListDef(listFieldSchema));
					break;
				case MICRONODE:
					root.field(fields.getMicronodeDef(fieldSchema, project));
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
