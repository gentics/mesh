package com.gentics.mesh.graphql.filter;

import static com.gentics.graphqlfilter.util.FilterUtil.nullablePredicate;
import static graphql.Scalars.GraphQLString;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.operation.Comparison;

import graphql.schema.GraphQLList;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.reactivex.json.schema.JsonSchema;
import io.vertx.reactivex.json.schema.Validator;

public class JsonFilter extends MainFilter<JsonObject> {

	private static final Logger log = LoggerFactory.getLogger(JsonFilter.class);

	private static JsonFilter instance;

	/**
	 * Filters JSON strings by various means
	 */
	public static synchronized JsonFilter filter() {
		if (instance == null) {
			instance = new JsonFilter(null);
		}
		return instance;
	}

	public JsonFilter(String owner) {
		super("JsonFilter", "Filters Jsons", true, Optional.ofNullable(owner));
	}

	@Override
	protected List<FilterField<JsonObject, ?>> getFilters() {
		return Arrays.asList(
			FilterField.isNull(),
			FilterField.create("equals", "Compares the object to the given JSON object for equality.", GraphQLString,
				objectValuePredicate(JsonObject::equals),
				Optional.of(query -> Comparison.eq(query.makeFieldOperand(Optional.empty()), query.makeValueOperand(true, JsonFilter::parseJson), query.getInitiatingFilterName()))),
			FilterField.create("notEquals", "Compares the object to the given JSON object for inequality.", GraphQLString,
					objectValuePredicate((i1, i2) -> !Objects.equals(i1, i2)),
					Optional.of(query -> Comparison.ne(query.makeFieldOperand(Optional.empty()), query.makeValueOperand(true, JsonFilter::parseJson), query.getInitiatingFilterName()))),
			FilterField.create("oneOf", "Tests if the object is equal to one of the given JSON objects.", GraphQLList.list(GraphQLString),
				this::oneOf, 
				Optional.of(query -> Comparison.in(query.makeFieldOperand(Optional.empty()), query.makeValueOperand(true, JsonFilter::parseJsons), query.getInitiatingFilterName()))),
			FilterField.create("hasSchema", "Tests if the object has the given JSON schema.", GraphQLString, objectSchemaPredicate(),
				Optional.empty()));
	}

	private Function<String, Predicate<JsonObject>> objectValuePredicate(BiPredicate<JsonObject, JsonObject> predicate) {
		return query -> {
			JsonObject queryJson = parseJson(query);
			return nullablePredicate(object -> predicate.test(object, queryJson));
		};
	}

	private Function<String, Predicate<JsonObject>> objectSchemaPredicate() {
		return query -> {
			JsonSchema schema = JsonSchema.of(new JsonObject(query));
			Validator validator = Validator.create(schema, new JsonSchemaOptions());
			return nullablePredicate(object -> validator.validate(object).getValid() == Boolean.TRUE);
		};
	}

	private Predicate<JsonObject> oneOf(List<String> query) {
		Set<JsonObject> objects = query.stream()
			.map(JsonFilter::parseJson)
			.collect(Collectors.toSet());

		return nullablePredicate(object -> objects.stream().anyMatch(jo -> jo.equals(object)));
	}

	public static Collection<JsonObject> parseJsons(Collection<String> objects) {
		return objects.stream().map(JsonFilter::parseJson).collect(Collectors.toList());
	}

	public static JsonObject parseJson(String object) {
		Object decoded = Json.decodeValue(object);
		if (decoded instanceof JsonObject o) {
			return o;
		} else {
			throw new IllegalStateException("Value is not a JSON object: %s".formatted(object));
		}
	}
}
