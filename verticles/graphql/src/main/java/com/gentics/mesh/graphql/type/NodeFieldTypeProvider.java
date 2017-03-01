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

import graphql.schema.GraphQLFieldDefinition;
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

	public GraphQLUnionType getFieldsType(Project project) {
		Map<String, GraphQLObjectType> types = generateFieldType(project);
		// System.out.println("After: " + types[0].hashCode());
		// System.out.println("After: " + types[0].getName());

		GraphQLObjectType[] typeArray = types.values().toArray(new GraphQLObjectType[types.values().size()]);

		GraphQLUnionType fieldType = newUnionType().name("Fields").possibleTypes(typeArray)
				.description("Fields of the node.").typeResolver(object -> {
					if (object instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer fieldContainer = (NodeGraphFieldContainer) object;
						return types.get(fieldContainer.getSchemaContainerVersion().getName());
					}
					return null;
				}).build();
		
		//.language
//		nodeType.field(newFieldDefinition().name("language")
//				.type(GraphQLString)
//				.dataFetcher(fetcher -> {
//					Object source = fetcher.getSource();
//					if (source instanceof Node) {
//						// TODO implement correct language handling
//						return ((Node) source).getGraphFieldContainer("en")
//								.getLanguage()
//								.getLanguageTag();
//					}
//					return null;
//				})
//				.build());

		
		return fieldType;

		// GraphQLUnionType PetType = newUnionType().name("Fields").possibleTypes(types).typeResolver(new TypeResolver() {
		// @Override
		// public GraphQLObjectType getType(Object object) {
		//
		// GraphQLObjectType type = schemaTypes.get("content");
		// System.out.println(type.hashCode());
		// System.out.println(types.get[0].hashCode());
		// System.out.println(type.getName());
		// return type;
		// }
		// }).build();
		//
		// return PetType;
	}

	public Map<String, GraphQLObjectType> generateFieldType(Project project) {
		Map<String, GraphQLObjectType> schemaTypes = new HashMap<>();
		List<GraphQLObjectType> list = new ArrayList<>();
		for (SchemaContainer container : project.getSchemaContainerRoot()
				.findAll()) {
			SchemaContainerVersion version = container.getLatestVersion();
			Schema schema = version.getSchema();
			Builder root = newObject();
			root.name(version.getName());
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
					ListFieldSchema listFieldSchema = ((ListFieldSchema)fieldSchema);
					root.field(fields.getListDef(listFieldSchema));
					break;
				case MICRONODE:
					root.field(fields.getMicronodeDef(fieldSchema));
					break;
				}

			}
			GraphQLObjectType type = root.build();
			list.add(type);
			schemaTypes.put(schema.getName(), type);
		}
		return schemaTypes;
	}

	private GraphQLFieldDefinition getFieldDefinition(String name, String label) {
		// TODO Auto-generated method stub
		return null;
	}

}
