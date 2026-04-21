package com.gentics.mesh.graphql.filter;

import static com.gentics.graphqlfilter.util.FilterUtil.nullablePredicate;
import static graphql.Scalars.GraphQLString;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.graphqlfilter.filter.FilterField;
import com.gentics.graphqlfilter.filter.MainFilter;
import com.gentics.graphqlfilter.filter.operation.Comparison;
import com.gentics.mesh.core.rest.node.field.JsonContent;
import com.gentics.mesh.json.JsonUtil;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;

import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.json.schema.JsonSchema;
import io.vertx.reactivex.json.schema.Validator;

public class JsonFilter extends MainFilter<JsonContent> {

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
	protected List<FilterField<JsonContent, ?>> getFilters() {
		return Arrays.asList(
			FilterField.isNull(),
			FilterField.<JsonContent, String>create("like", "Checks if the JSON object matches the given SQL LIKE expression.", GraphQLString, likePredicate(),
				Optional.of((query) -> Comparison.like(query.makeFieldOperand(Optional.empty()), query.makeValueOperand(true), query.getInitiatingFilterName()))),
			FilterField.<JsonContent, String>create("regex", "Checks if the JSON object representation matches the given regular expression.", GraphQLString, regexPredicate(),
				Optional.empty()),
			FilterField.create("hasSchema", "Tests if the object has the given JSON schema.", GraphQLString, objectSchemaPredicate(),
				Optional.empty()),
			FilterField.create("jsonPath", "Tests the given JSON schema against JsonPath value.", GraphQLString, jsonPathPredicate(),
				Optional.empty()));
	}

	private Function<String, Predicate<JsonContent>> jsonPathPredicate() {
		return query -> {
			JsonPath jsonPath = JsonPath.compile(query);
			JsonProvider provider = new JacksonJsonProvider(JsonUtil.getMapper());
			ParseContext context = JsonPath.using(provider);
			return nullablePredicate(object -> {
				List<?> parsed = context.parse(JsonUtil.toJson(object)).read(jsonPath, List.class);
				return parsed != null && !parsed.isEmpty();
			});
		};
	}

	/**
	 * Parse a collection of JSON objects
	 * 
	 * @param objects
	 * @return
	 */
	public static Collection<JsonContent> parseJsons(Collection<String> objects) {
		return objects.stream().map(JsonFilter::parseJson).collect(Collectors.toList());
	}

	/**
	 * Try parse the given string to a JSOB object, or return an empty one.
	 * 
	 * @param object
	 * @return
	 */
	public static JsonContent parseJson(String object) {
		try {
			Object decoded = JsonUtil.readValue(object, JsonContent.class);
			if (decoded instanceof JsonContent o) {
				return o;
			}
		} catch (DecodeException e) {
			log.warn("JSON decode failed for " + object, e);
		}
		return new JsonContent();
	}

	private String likeToRegex(String likeQuery) {
		return likeQuery
				.replace("\\.", DOT_PLACEHOLDER).replace("\\%", PERCENT_PLACEHOLDER).replace("\\_", UNDERSCORE_PLACEHOLDER)
				.replace(".", "\\.").replace("%", ".*?").replace("_", ".")
				.replace(DOT_PLACEHOLDER, ".").replace(PERCENT_PLACEHOLDER, "%").replace(UNDERSCORE_PLACEHOLDER, "_");
	}

	private Function<String, Predicate<JsonContent>> objectSchemaPredicate() {
		return query -> {
			JsonSchema schema = JsonSchema.of(new JsonObject(query));
			Validator validator = JsonUtil.newJsonSchemaValidator(schema);
			return nullablePredicate(object -> {
				Object jsonContent = object.isArray() ? object.getArray() : object.getObject();
				return validator.validate(jsonContent).getValid() == Boolean.TRUE;
			});
		};
	}

	private Function<String, Predicate<JsonContent>> likePredicate() {
		return query -> {
			Pattern regex = Pattern.compile(likeToRegex(query));
			return nullablePredicate(object -> regex.matcher(JsonUtil.toJson(object)).find());
		};
	}

	private Function<String, Predicate<JsonContent>> regexPredicate() {
		return query -> {
			Pattern regex = Pattern.compile(query);
			return nullablePredicate(object -> regex.matcher(JsonUtil.toJson(object)).find());
		};
	}
}
