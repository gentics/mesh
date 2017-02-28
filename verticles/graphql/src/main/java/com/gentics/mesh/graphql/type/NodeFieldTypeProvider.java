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
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.graphql.type.field.BinaryFieldTypeProvider;
import com.gentics.mesh.graphql.type.field.BooleanFieldTypeProvider;
import com.gentics.mesh.graphql.type.field.DateFieldTypeProvider;
import com.gentics.mesh.graphql.type.field.HtmlFieldTypeProvider;
import com.gentics.mesh.graphql.type.field.ListFieldTypeProvider;
import com.gentics.mesh.graphql.type.field.MicronodeFieldTypeProvider;
import com.gentics.mesh.graphql.type.field.NumberFieldTypeProvider;
import com.gentics.mesh.graphql.type.field.StringFieldTypeProvider;

import dagger.Lazy;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLUnionType;

@Singleton
public class NodeFieldTypeProvider extends AbstractTypeProvider {

	@Inject
	public StringFieldTypeProvider stringFieldProvider;

	@Inject
	public HtmlFieldTypeProvider htmlFieldProvider;

	@Inject
	public DateFieldTypeProvider dateFieldProvider;

	@Inject
	public NumberFieldTypeProvider numberFieldProvider;

	@Inject
	public BooleanFieldTypeProvider booleanFieldProvider;

	@Inject
	public Lazy<NodeFieldTypeProvider> nodeFieldProvider;

	@Inject
	public MicronodeFieldTypeProvider micronodeFieldProvider;	
	
	@Inject
	public BinaryFieldTypeProvider binaryFieldProvider;
	
	@Inject
	public ListFieldTypeProvider listFieldProvider;
	
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
		for (SchemaContainer container : project.getSchemaContainerRoot().findAll()) {
			SchemaContainerVersion version = container.getLatestVersion();
			Schema schema = version.getSchema();
			Builder root = newObject();
			root.name(version.getName());
			for (FieldSchema fieldSchema : schema.getFields()) {
				FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
				switch (type) {
				case STRING:
					root.field(stringFieldProvider.getFieldDefinition(fieldSchema.getName(), fieldSchema.getLabel()));
					break;
				case HTML:
					root.field(htmlFieldProvider.getFieldDefinition(fieldSchema.getName(), fieldSchema.getLabel()));
					break;
				case NUMBER:
					root.field(numberFieldProvider.getFieldDefinition(fieldSchema.getName(), fieldSchema.getLabel()));
					break;
				case DATE:
					root.field(dateFieldProvider.getFieldDefinition(fieldSchema.getName(), fieldSchema.getLabel()));
					break;
				case BOOLEAN:
					root.field(booleanFieldProvider.getFieldDefinition(fieldSchema.getName(), fieldSchema.getLabel()));
					break;
				case NODE:
					root.field(nodeFieldProvider.get().getFieldDefinition(fieldSchema.getName(), fieldSchema.getLabel()));
					break;
				case BINARY:
					//root.field(binaryFieldProvider.getFieldDefinition(fieldSchema.getName(), fieldSchema.getLabel()));
					break;
				case LIST:
					root.field(listFieldProvider.getFieldDefinition(fieldSchema.getName(), fieldSchema.getLabel()));
					break;
				case MICRONODE:
					root.field(micronodeFieldProvider.getFieldDefinition(fieldSchema.getName(), fieldSchema.getLabel()));
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
