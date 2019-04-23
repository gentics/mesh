package com.gentics.mesh.graphql.type.field;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.type.AbstractTypeProvider;
import com.gentics.mesh.graphql.type.InterfaceTypeProvider;
import com.gentics.mesh.graphql.type.NodeTypeProvider;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLTypeReference;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

import static com.gentics.mesh.graphql.type.NodeTypeProvider.NODE_TYPE_NAME;
import static graphql.schema.GraphQLObjectType.newObject;

/**
 * This class contains all field specific type code. It provides methods which can be used to generate field types for a specific project. This is done by
 * examining the schemas which are assigned to the project branch.
 */
@Singleton
public class NodeFieldTypeProvider extends AbstractTypeProvider {

	public static final String NODE_FIELDS_TYPE_NAME = "Fields";

	@Inject
	public FieldDefinitionProvider fields;

	@Inject
	public NodeTypeProvider nodeTypeProvider;

	@Inject
	public InterfaceTypeProvider interfaceTypeProvider;

	@Inject
	public NodeFieldTypeProvider() {
	}

	/**
	 * Generate a map of all schema types which correspond to schemas which are part of the project/branch.
	 * 
	 * @param context
	 * @return
	 */
	public List<GraphQLObjectType> generateSchemaFieldTypes(GraphQLContext context) {
		Project project = context.getProject();

		return project.getSchemaContainerRoot().findAll().stream().map(container -> {
			SchemaContainerVersion version = container.getLatestVersion();
			Schema schema = version.getSchema();
			Builder root = newObject();
			root.withInterface(GraphQLTypeReference.typeRef(NODE_TYPE_NAME));
			root.name(schema.getName());
			root.description(schema.getDescription());

			GraphQLFieldDefinition.Builder fieldsField = GraphQLFieldDefinition.newFieldDefinition();
			Builder fieldsType = newObject();
			fieldsType.name(nodeTypeName(schema.getName()));
			fieldsField.dataFetcher(env -> {
				NodeContent content = env.getSource();
				return content.getContainer();
			});

			// TODO add link resolving argument / code
			for (FieldSchema fieldSchema : schema.getFields()) {
				FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());
				switch (type) {
				case STRING:
					fieldsType.field(fields.createStringDef(fieldSchema));
					break;
				case HTML:
					fieldsType.field(fields.createHtmlDef(fieldSchema));
					break;
				case NUMBER:
					fieldsType.field(fields.createNumberDef(fieldSchema));
					break;
				case DATE:
					fieldsType.field(fields.createDateDef(fieldSchema));
					break;
				case BOOLEAN:
					fieldsType.field(fields.createBooleanDef(fieldSchema));
					break;
				case NODE:
					fieldsType.field(fields.createNodeDef(fieldSchema));
					break;
				case BINARY:
					fieldsType.field(fields.createBinaryDef(fieldSchema));
					break;
				case LIST:
					ListFieldSchema listFieldSchema = ((ListFieldSchema) fieldSchema);
					fieldsType.field(fields.createListDef(context, listFieldSchema));
					break;
				case MICRONODE:
					fieldsType.field(fields.createMicronodeDef(fieldSchema, project));
					break;
				}
			}
			fieldsField.name("fields").type(fieldsType);
			root.field(fieldsField);
			root.fields(nodeTypeProvider.createNodeInterfaceFields(context));
			interfaceTypeProvider.addCommonFields(root, true);
			return root.build();
		}).collect(Collectors.toList());
	}

	public static String nodeTypeName(String schemaName) {
		return schemaName + "Fields";
	}

}
