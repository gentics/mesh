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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.operation.Comparison;
import com.gentics.mesh.json.JsonUtil;

import graphql.schema.GraphQLList;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.OutputFormat;
import io.vertx.reactivex.json.schema.JsonSchema;
import io.vertx.reactivex.json.schema.Validator;

public class JsonFilter extends MainFilter<JsonObject> {

	private static final Logger log = LoggerFactory.getLogger(JsonFilter.class);

	private static final String UNDERSCORE_PLACEHOLDER = "\\{UNS}\\";
	private static final String PERCENT_PLACEHOLDER = "\\{REM}\\";
	private static final String DOT_PLACEHOLDER = "\\{DOT}\\";
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
			FilterField.<JsonObject, String>create("contains", "Checks if the string contains the given substring.", GraphQLString, 
				query -> nullablePredicate(input -> JsonUtil.toJson(input).contains(query)),
				Optional.empty()),
			FilterField.create("oneOf", "Tests if the object is equal to one of the given JSON objects.", GraphQLList.list(GraphQLString),
				this::oneOf, 
				Optional.of(query -> Comparison.in(query.makeFieldOperand(Optional.empty()), query.makeValueOperand(true, JsonFilter::parseJsons), query.getInitiatingFilterName()))),
			FilterField.<JsonObject, String>create("like", "Checks if the JSON object matches the given SQL LIKE expression.", GraphQLString, likePredicate(),
				Optional.of((query) -> Comparison.like(query.makeFieldOperand(Optional.empty()), query.makeValueOperand(true), query.getInitiatingFilterName()))),
			FilterField.<JsonObject, String>create("regex", "Checks if the JSON object representation matches the given regular expression.", GraphQLString, regexPredicate(),
				Optional.empty()),
			FilterField.create("hasSchema", "Tests if the object has the given JSON schema.", GraphQLString, objectSchemaPredicate(),
				Optional.empty()));
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

	/**
	 * Try parse the given string to a JSOB object, or return an empty one.
	 * 
	 * @param object
	 * @return
	 */
	public static JsonObject parseJson(String object) {
		try {
			Object decoded = Json.decodeValue(object);
			if (decoded instanceof JsonObject o) {
				return o;
			}
		} catch (DecodeException e) {
			log.warn("JSON decode failed for " + object, e);
		}
		return new JsonObject();
	}

	private String likeToRegex(String likeQuery) {
		return likeQuery
				.replace("\\.", DOT_PLACEHOLDER).replace("\\%", PERCENT_PLACEHOLDER).replace("\\_", UNDERSCORE_PLACEHOLDER)
				.replace(".", "\\.").replace("%", ".*?").replace("_", ".")
				.replace(DOT_PLACEHOLDER, ".").replace(PERCENT_PLACEHOLDER, "%").replace(UNDERSCORE_PLACEHOLDER, "_");
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
			Validator validator = Validator.create(schema, new JsonSchemaOptions().setBaseUri("https://gentics.com/mesh").setDraft(Draft.DRAFT202012));
			return nullablePredicate(object -> validator.validate(object).getValid() == Boolean.TRUE);
		};
	}

	private Function<String, Predicate<JsonObject>> likePredicate() {
		return query -> {
			Pattern regex = Pattern.compile(likeToRegex(query));
			return nullablePredicate(object -> regex.matcher(JsonUtil.toJson(object)).find());
		};
	}

	private Function<String, Predicate<JsonObject>> regexPredicate() {
		return query -> {
			Pattern regex = Pattern.compile(query);
			return nullablePredicate(object -> regex.matcher(JsonUtil.toJson(object)).find());
		};
	}
}
