package com.gentics.mesh.graphql;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLUnionType.newUnionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLUnionType;
import graphql.schema.TypeResolver;

public class GarfieldSchema {

	public interface Named {
		String getName();
	}

	public static class Dog implements Named {
		private final String name;

		private final boolean barks;

		public Dog(String name, boolean barks) {
			this.name = name;
			this.barks = barks;
		}

		public boolean isBarks() {
			return barks;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public static class Cat implements Named {
		private final String name;

		private final boolean meows;

		public Cat(String name, boolean meows) {
			this.name = name;
			this.meows = meows;
		}

		public boolean isMeows() {
			return meows;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public static class Person implements Named {
		private final String name;
		private List<Dog> dogs;
		private List<Cat> cats;
		private List<Named> friends;

		public Person(String name) {
			this(name, Collections.<Cat> emptyList(), Collections.<Dog> emptyList(), Collections.<Named> emptyList());
		}

		public Person(String name, List<Cat> cats, List<Dog> dogs, List<Named> friends) {
			this.name = name;
			this.dogs = dogs;
			this.cats = cats;
			this.friends = friends;
		}

		public List<Object> getPets() {
			List<Object> pets = new ArrayList<Object>();
			pets.addAll(cats);
			pets.addAll(dogs);
			return pets;
		}

		@Override
		public String getName() {
			return name;
		}

		public List<Named> getFriends() {
			return friends;
		}
	}

	public static Cat garfield = new Cat("Garfield", false);
	public static Dog odie = new Dog("Odie", true);
	public static Person liz = new Person("Liz");
	public static Person john = new Person("John", Arrays.asList(garfield), Arrays.asList(odie), Arrays.asList(liz, odie));

	public static GraphQLInterfaceType FieldType = newInterface().name("Named").field(newFieldDefinition().name("name").type(GraphQLString).build())
			.typeResolver(new TypeResolver() {
				@Override
				public GraphQLObjectType getType(Object object) {
					if (object instanceof Dog) {
						return StringType;
					}
					if (object instanceof Person) {
						return PersonType;
					}
					if (object instanceof Cat) {
						return NodeType;
					}
					return null;
				}
			}).build();

	public static GraphQLObjectType StringType = newObject().name("String").field(newFieldDefinition().name("name").type(GraphQLString).build())
			.field(newFieldDefinition().name("barks").type(GraphQLBoolean).build()).withInterface(FieldType).build();

	public static GraphQLObjectType NodeType = newObject().name("Node").field(newFieldDefinition().name("name").type(GraphQLString).build())
			.field(newFieldDefinition().name("meows").type(GraphQLBoolean).build()).withInterface(FieldType).build();

	public static GraphQLUnionType PetType = newUnionType().name("Pet").possibleType(NodeType).possibleType(StringType)
			.typeResolver(new TypeResolver() {
				@Override
				public GraphQLObjectType getType(Object object) {
					if (object instanceof Cat) {
						return NodeType;
					}
					if (object instanceof Dog) {
						return StringType;
					}
					return null;
				}
			}).build();

	public static GraphQLFieldDefinition fieldsField = newFieldDefinition().name("friends").type(new GraphQLList(FieldType))
			.argument(newArgument().name("uuid").description("UUid of the field").type(GraphQLString).build()).dataFetcher(fetcher -> {
				System.out.println(fetcher.getSource().getClass().getName());
				String uuid = fetcher.getArgument("uuid");
				System.out.println(uuid);
				return john.getFriends();
			}).build();

	public static GraphQLObjectType PersonType = newObject().name("Person").field(newFieldDefinition().name("name").type(GraphQLString).build())
			// .field(newFieldDefinition().name("fieldName").type(GraphQLString).staticValue("blar").build())
			// .field(newFieldDefinition().name("pets").type(new GraphQLList(PetType)).build())
			.field(newFieldDefinition().name("firstFriend").type(FieldType).dataFetcher(fetcher -> {
				return john.getFriends().get(0);
			}).build()).field(fieldsField).withInterface(FieldType).build();

	public static GraphQLSchema GarfieldSchema = GraphQLSchema.newSchema().query(PersonType).build();

}