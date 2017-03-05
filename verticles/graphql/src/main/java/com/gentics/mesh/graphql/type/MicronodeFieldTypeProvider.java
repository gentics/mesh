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
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
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

		GraphQLUnionType fieldType = newUnionType().name("MicronodeFields")
				.possibleTypes(typeArray)
				.description("Fields of the micronode.")
				.typeResolver(object -> {
					if (object instanceof Micronode) {
						Micronode fieldContainer = (Micronode) object;
						MicroschemaContainerVersion micronodeFieldSchema = fieldContainer.getSchemaContainerVersion();
						return types.get(micronodeFieldSchema.getName());
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
			System.out.println(microschema.getName());
			root.description(microschema.getDescription());

			for (FieldSchema fieldSchema : microschema.getFields()) {
				FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
				switch (type) {
				case STRING:
					root.field(fields.get()
							.getStringDef(fieldSchema));
					break;
				case HTML:
					root.field(fields.get()
							.getHtmlDef(fieldSchema));
					break;
				case NUMBER:
					root.field(fields.get()
							.getNumberDef(fieldSchema));
					break;
				case DATE:
					root.field(fields.get()
							.getDateDef(fieldSchema));
					break;
				case BOOLEAN:
					root.field(fields.get()
							.getBooleanDef(fieldSchema));
					break;
				case NODE:
					root.field(fields.get()
							.getNodeDef(fieldSchema));
					break;
				case LIST:
					ListFieldSchema listFieldSchema = ((ListFieldSchema) fieldSchema);
					root.field(fields.get()
							.getListDef(listFieldSchema));
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
