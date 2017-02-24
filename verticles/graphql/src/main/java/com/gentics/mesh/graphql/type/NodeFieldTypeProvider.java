package com.gentics.mesh.graphql.type;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
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
import com.gentics.mesh.core.rest.schema.Schema;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLUnionType;

@Singleton
public class NodeFieldTypeProvider {

	private StringFieldTypeProvider stringFieldProvider;

	private DateFieldTypeProvider dateFieldProvider;

	@Inject
	public NodeFieldTypeProvider(StringFieldTypeProvider stringFieldProvider, DateFieldTypeProvider dateFieldProvider) {
		this.stringFieldProvider = stringFieldProvider;
		this.dateFieldProvider = dateFieldProvider;
	}

	public static Map<String, GraphQLObjectType> schemaTypes = new HashMap<>();

	public GraphQLUnionType getFieldsType(Project project) {
		GraphQLObjectType[] types = generateFieldType(project);
		GraphQLUnionType fieldType = newUnionType().name("Fields").possibleTypes(types).description("Fields of the node.")
				.typeResolver(object -> {
					if (object instanceof NodeGraphFieldContainer) {
						NodeGraphFieldContainer fieldContainer = (NodeGraphFieldContainer) object;
						return schemaTypes.get(fieldContainer.getSchemaContainerVersion().getName());
					}
					return null;
				}).build();
		return fieldType;
	}

	public GraphQLObjectType[] generateFieldType(Project project) {

//		public static GraphQLObjectType NodeType = newObject().name("Node").field(newFieldDefinition().name("name").type(GraphQLString).build())
//				.field(newFieldDefinition().name("meows").type(GraphQLBoolean).build()).withInterface(FieldType).build();
		
		List<GraphQLObjectType> list = new ArrayList<>();
		for (SchemaContainer container : project.getSchemaContainerRoot().findAll()) {
			SchemaContainerVersion version = container.getLatestVersion();
			Schema schema = version.getSchema();
			Builder root = newObject();
			root.name(version.getName());
			root.field(newFieldDefinition().name("title").type(GraphQLString).staticValue("blar").build());
//			for (FieldSchema fieldSchema : schema.getFields()) {
//				root.field(newFieldDefinition().name(fieldSchema.getName()).description(fieldSchema.getLabel()).type(GraphQLString).dataFetcher(fetcher -> {
//					System.out.println("adgasdgasdg");
//					return null;
//				}).build());
//			}
			GraphQLObjectType type = root.build();
			list.add(type);
			schemaTypes.put(schema.getName(), type);
		}

		return list.toArray(new GraphQLObjectType[list.size()]);
	}

}
