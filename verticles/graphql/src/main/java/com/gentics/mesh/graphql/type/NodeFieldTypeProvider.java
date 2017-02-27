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
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;

import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import graphql.schema.GraphQLUnionType;
import graphql.schema.TypeResolver;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;

@Singleton
public class NodeFieldTypeProvider {

	private StringFieldTypeProvider stringFieldProvider;

	private DateFieldTypeProvider dateFieldProvider;

	@Inject
	public NodeFieldTypeProvider(StringFieldTypeProvider stringFieldProvider, DateFieldTypeProvider dateFieldProvider) {
		this.stringFieldProvider = stringFieldProvider;
		this.dateFieldProvider = dateFieldProvider;
	}



	public GraphQLUnionType getFieldsType(Project project) {
		Map<String,GraphQLObjectType> types = generateFieldType(project);
//		System.out.println("After: " + types[0].hashCode());
//		System.out.println("After: " + types[0].getName());
		
		
		GraphQLObjectType[] typeArray =types.values().toArray(new GraphQLObjectType[types.values().size()]);

		GraphQLUnionType fieldType = newUnionType().name("Fields").possibleTypes(typeArray).description("Fields of the node.").typeResolver(object -> {
			if (object instanceof NodeGraphFieldContainer) {
					NodeGraphFieldContainer fieldContainer = (NodeGraphFieldContainer) object;
					return types.get(fieldContainer.getSchemaContainerVersion().getName());
				}
				return null;
			}).build();
		return fieldType;

//		GraphQLUnionType PetType = newUnionType().name("Fields").possibleTypes(types).typeResolver(new TypeResolver() {
//			@Override
//			public GraphQLObjectType getType(Object object) {
//
//				GraphQLObjectType type = schemaTypes.get("content");
//				System.out.println(type.hashCode());
//				System.out.println(types.get[0].hashCode());
//				System.out.println(type.getName());
//				return type;
//			}
//		}).build();
//
//		return PetType;
	}

	public Map<String,GraphQLObjectType> generateFieldType(Project project) {

		Map<String, GraphQLObjectType> schemaTypes = new HashMap<>();
		
		//		GraphQLObjectType DogType = newObject().name("content").field(newFieldDefinition().name("name").type(GraphQLString).staticValue("connntent"))
		//				.field(newFieldDefinition().name("barks").type(GraphQLBoolean)).build();
		//
		//		GraphQLObjectType CatType = newObject().name("folder").field(newFieldDefinition().name("name").type(GraphQLString).staticValue("fooollder"))
		//				.field(newFieldDefinition().name("meows").type(GraphQLBoolean)).build();

		//		public static GraphQLObjectType NodeType = newObject().name("Node").field(newFieldDefinition().name("name").type(GraphQLString).build())
		//				.field(newFieldDefinition().name("meows").type(GraphQLBoolean).build()).withInterface(FieldType).build();

		List<GraphQLObjectType> list = new ArrayList<>();
		for (SchemaContainer container : project.getSchemaContainerRoot().findAll()) {
			SchemaContainerVersion version = container.getLatestVersion();
			Schema schema = version.getSchema();
			Builder root = newObject();
			root.name(version.getName());
			//			root.field(newFieldDefinition().name("title").type(GraphQLString).staticValue("blar").build());
			for (FieldSchema fieldSchema : schema.getFields()) {
				//TODO select correct field type
				root.field(newFieldDefinition().name(fieldSchema.getName()).description(fieldSchema.getLabel()).type(GraphQLString)
						.dataFetcher(fetcher -> {
							Object context = fetcher.getContext();
							return "1234";
						}).build());
			}
			GraphQLObjectType type = root.build();
			list.add(type);
			schemaTypes.put(schema.getName(), type);
		}

		//		list.add(DogType);
		//		list.add(CatType);
		//		schemaTypes.put("content", DogType);
		//		schemaTypes.put("folder", CatType);
		//
		//		System.out.println("Before:" + DogType.hashCode());
		//		System.out.println("Before:" + DogType.getName());

		return schemaTypes;
	}

}
