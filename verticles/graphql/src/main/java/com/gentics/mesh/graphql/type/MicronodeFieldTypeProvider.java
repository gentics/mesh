package com.gentics.mesh.graphql.type;

import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLUnionType.newUnionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.graphql.type.field.FieldDefinitionProvider;

import dagger.Lazy;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLUnionType;

@Singleton
public class MicronodeFieldTypeProvider extends AbstractTypeProvider {

	@Inject
	public Lazy<FieldDefinitionProvider> fields;

	@Inject
	public MicronodeFieldTypeProvider() {
	}

	public GraphQLUnionType getMicroschemaFieldsType(Project project) {

		Map<String, GraphQLObjectType> types = generateMicroschemaFieldType(project);

		GraphQLObjectType[] typeArray = types.values()
				.toArray(new GraphQLObjectType[types.values()
						.size()]);

		GraphQLUnionType fieldType = newUnionType().name("Micronode")
				.possibleTypes(typeArray)
				.description("Fields of the micronode.")
				.typeResolver(object -> {
					if (object instanceof Micronode) {
						Micronode fieldContainer = (Micronode) object;
						MicroschemaContainerVersion micronodeFieldSchema = fieldContainer.getSchemaContainerVersion();
						String schemaName = micronodeFieldSchema.getName();
						GraphQLObjectType foundType = types.get(schemaName);
						return foundType;
					}
					return null;
				})
				.build();

		return fieldType;
	}

	static Map<String, GraphQLObjectType> schemaTypes = new HashMap<>();

	private Map<String, GraphQLObjectType> generateMicroschemaFieldType(Project project) {

		if (!schemaTypes.isEmpty()) {
			return schemaTypes;
		}

		List<GraphQLObjectType> list = new ArrayList<>();
		for (MicroschemaContainer container : project.getMicroschemaContainerRoot()
				.findAll()) {
			MicroschemaContainerVersion version = container.getLatestVersion();
			Microschema microschema = version.getSchema();
			Builder root = newObject();
			root.name(microschema.getName());
			root.description(microschema.getDescription());

			for (FieldSchema fieldSchema : microschema.getFields()) {
				FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
				switch (type) {
				case STRING:
					root.field(fields.get()
							.createStringDef(fieldSchema));
					break;
				case HTML:
					root.field(fields.get()
							.createHtmlDef(fieldSchema));
					break;
				case NUMBER:
					root.field(fields.get()
							.createNumberDef(fieldSchema));
					break;
				case DATE:
					root.field(fields.get()
							.createDateDef(fieldSchema));
					break;
				case BOOLEAN:
					root.field(fields.get()
							.createBooleanDef(fieldSchema));
					break;
				case NODE:
					root.field(fields.get()
							.createNodeDef(fieldSchema));
					break;
				case LIST:
					ListFieldSchema listFieldSchema = ((ListFieldSchema) fieldSchema);
					root.field(fields.get()
							.createListDef(listFieldSchema));
					break;
				default:
					//TODO throw exception for unsupported type
					break;
				}

			}
			GraphQLObjectType type = root.build();
			list.add(type);
			schemaTypes.put(microschema.getName(), type);
		}
		return schemaTypes;
	}

}
