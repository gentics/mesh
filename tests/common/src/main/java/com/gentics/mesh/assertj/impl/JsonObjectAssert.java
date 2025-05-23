package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.AbstractAssert;
import org.jetbrains.annotations.NotNull;

import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.UUIDUtil;
import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonObjectAssert extends AbstractAssert<JsonObjectAssert, JsonObject> {
	/**
	 * Key to check
	 */
	protected String key;
	protected Comparator<String> sortComparator = (a,b) -> 0;

	private static Map<String, String> staticVariables = ImmutableMap.<String, String>builder()
			.put("CURRENT_API_BASE_PATH", CURRENT_API_BASE_PATH)
			.build();

	private Map<String, String> dynamicVariables = new HashMap<>();

	private static final String SORT_PATTERN = "<is-sorted-by:(?<id>[a-zA-Z0-9\\._\\-]*):(?<ord>asc|desc)>";

	public JsonObjectAssert(JsonObject actual) {
		super(actual, JsonObjectAssert.class);
	}

	public JsonObjectAssert key(String key) {
		this.key = key;
		return this;
	}

	public JsonObjectAssert withSortComparator(Comparator<String> comparator) {
		this.sortComparator = comparator;
		return this;
	}

	/**
	 * The assert will try to substitute assertion strings composed like %key% to value
	 * @param key
	 * @param value
	 * @return
	 */
	public JsonObjectAssert replacingPlaceholderVariable(String key, String value) {
		dynamicVariables.put(key, value);
		return this;
	}

	public JsonObjectAssert matches(Object expected) {
		assertNotNull(descriptionText() + " cannot be matched without specifying key first", key);
		assertNotNull(descriptionText() + " JsonObject must not be null", actual);
		assertEquals(descriptionText() + " key " + key, expected, actual.getValue(key));
		return this;
	}

	public JsonObjectAssert hasNot(String path, String msg) {
		try {
			getByPath(path);
			fail("Could not find property for path {" + path + "} - Json is:\n--snip--\n" + actual.encodePrettily() + "\n--snap--\n" + msg);
		} catch (PathNotFoundException e) {
			// Okay
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public JsonObjectAssert has(String path, String value, String msg) {
		try {
			Object actualValue = getByPath(path);
			if (actualValue instanceof Collection<?>) {
				Collection<Object> actualCollection = (Collection<Object>) actualValue;

				if (StringUtils.startsWith(value, "[") && StringUtils.endsWith(value, "]")) {
					value = StringUtils.removeStart(value, "[");
					value = StringUtils.removeEnd(value, "]");
					String[] valueParts = StringUtils.split(value, ",");
					for (int i = 0; i < valueParts.length; i++) {
						valueParts[i] = StringUtils.trim(valueParts[i]);
					}
					List<String> values = Arrays.asList(valueParts);
					assertThat(actualCollection).as("Value for property on path {" + path + "}").containsOnlyElementsOf(values);
				} else {
					fail("Expected value for path {" + path + "} should be an array (eclosed by '[' and ']') but was {"
							+ value + "}");
				}

			} else {
				String actualStringRep = String.valueOf(actualValue);
				assertEquals("Value for property on path {" + path + "} did not match: " + msg, value, actualStringRep);
			}
		} catch (PathNotFoundException e) {
			fail("Could not find property for path {" + path + "} - Json is:\n--snip--\n" + actual.encodePrettily() + "\n--snap--\n" + msg);
		}
		return this;
	}

	/**
	 * Resolve the given JSON path to load the value.
	 *
	 * @param jsonPath
	 *            the JSON path
	 * @param <T>
	 *            expected return type
	 * @return list of objects matched by the given path
	 */
	private <T> T getByPath(String jsonPath) {
		return JsonPath.read(actual.toString(), jsonPath);
	}

	public JsonObjectAssert hasNullValue(String key) {
		assertTrue("The json object should contain a map entry for key {" + key + "}", actual.containsKey(key));
		assertNull("The json object for key {" + key + "} should be null", actual.getJsonObject(key));
		return this;
	}

	/**
	 * Assert that the JSON object complies to the assertions which are stored in the comments of the GraphQL query with the given name.
	 *
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public JsonObjectAssert compliesToAssertions(String name) throws IOException {
		String path = "/graphql/" + name;
		InputStream ins = getClass().getResourceAsStream(path);
		if (ins == null) {
			fail("Could not find query file {" + path + "}");
		}
		return compliesToAssertions(ins);
	}

	public @NotNull JsonObjectAssert compliesToAssertions(String name, String version) {
		InputStream ins = Optional.ofNullable(getClass().getResourceAsStream("/graphql/" + name + "." + version))
			.orElseGet(() -> getClass().getResourceAsStream("/graphql/" + name));
		if (ins == null) {
			fail("Could not find query file {" + name + "}");
		}
		return compliesToAssertions(ins);
	}

	@NotNull
	private JsonObjectAssert compliesToAssertions(InputStream ins) {
		try (Scanner scanner = new Scanner(ins)) {
			compliesToAssertions(scanner);
		}
		return this;
	}

	public JsonObjectAssert compliesToAssertionText(String lines) {
		try (Scanner scanner = new Scanner(lines)) {
			compliesToAssertions(scanner);
		}
		return this;
	}

	private JsonObjectAssert compliesToAssertions(Scanner scanner) {
		int lineNr = 1;
		// Parse the query and extract comments which include assertions. Directly evaluate these assertions.
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			line = line.trim();
			if (line.startsWith("# [")) {
				int start = line.indexOf("# [") + 3;
				int end = line.lastIndexOf("]");
				String assertion = line.substring(start, end);
				evaluteAssertion(assertion, lineNr);
			}
			lineNr++;
		}
		return this;
	}

	private void evaluteAssertion(String assertion, int lineNr) {
		String msg = "Failure on line {" + lineNr + "}";
		compliesTo(assertion, msg, lineNr);
	}

	public JsonObjectAssert compliesTo(String assertion) {
		return compliesTo(assertion, null, null);
	}

	public JsonObjectAssert compliesTo(String assertion, String msg) {
		return compliesTo(assertion, msg, null);
	}

	public JsonObjectAssert compliesTo(String assertion, String msg, Integer lineNr) {
		String[] parts = assertion.split("=", 2);
		if (parts.length <= 1) {
			if (lineNr != null) {
				fail("Assertion on line {" + lineNr + "} is not complete {" + assertion + "}");
			} else {
				fail("Assertion {" + assertion + "} is not complete.");
			}
		}
		String path = parts[0];
		String value = parts[1];
		if (msg == null) {
			msg = "Assertion error in path";
		}

		if ("<not-null>".equals(value) || "<is-not-null>".equals(value)) {
			pathIsNotNull(path, msg);
		} else if ("<is-null>".equals(value)) {
			pathIsNull(path, msg);
		} else if ("<is-uuid>".equals(value)) {
			pathIsUuid(path, msg);
		} else if ("<is-date>".equals(value)) {
			pathIsDate(path, msg);
		} else if ("<is-undefined>".equals(value)) {
			pathIsUndefined(path, msg);
		} else if ("<is-empty>".equals(value)) {
			pathIsEmpty(path, msg);
		} else if (Pattern.matches(SORT_PATTERN, value)) {
			Matcher matcher = Pattern.compile(SORT_PATTERN).matcher(value);
			assert matcher.matches();
			String sortBy = matcher.group("id");
			String order = matcher.group("ord");
			pathMustBeSorted(path, sortBy, order, msg);
		} else {
			has(path, replaceVariables(value), msg);
		}
		return this;
	}

	public JsonObjectAssert pathMustBeSorted(String path, String sortBy, String order, String msg) {
		try {
			String object = actual.toString();
			com.google.gson.JsonArray arr = JsonPath.using(new GsonJsonProvider()).parse(object).read(path);
			List<String> sorted = IntStream.range(0, arr.size())
					.mapToObj(i -> {
						try {
							return Objects.toString(JsonPath.read(object, path + "[" + i + "]." + sortBy));
						} catch (Exception e) {
							return null;
						}
					})
					.collect(Collectors.toList());
			if (sorted.size() < 2) {
				System.out.println("WARN: too few elements to assert sorting in `" + path + "`: " + sorted);
				return this;
			}
			System.out.println(sorted);

			switch (order) {
			case "desc":
				assertThat(sorted).isSortedAccordingTo(sortComparator.reversed());
				break;
			default:
			case "asc":
				assertThat(sorted).isSortedAccordingTo(sortComparator);
				break;
			}		
		} catch (PathNotFoundException e) {
			fail(msg + " The value at path {" + path + "} must be sorted by {" + sortBy + "} : {" + order + "}.");
		}
		return this;
	}

	private String replaceVariables(String value) {
		Pattern pattern = Pattern.compile("%(.*)%");
		Matcher matcher = pattern.matcher(value);
		while (matcher.find()) {
			String varname = matcher.group(1);
			String replaceValue = staticVariables.getOrDefault(varname, dynamicVariables.getOrDefault(varname, null));
			value = matcher.replaceFirst(replaceValue);
			matcher = pattern.matcher(value);
		}
		return value;
	}

	/**
	 * Assert that the path is not present in the JSON object.
	 *
	 * @param path
	 * @param msg
	 * @return Fluent API
	 */
	public JsonObjectAssert pathIsUndefined(String path, String msg) {
		try {
			Object value = JsonPath.read(actual.toString(), path);
			System.out.println(value);
			fail(msg + " The value at path {" + path + "} was present but it should be undefined.");
		} catch (PathNotFoundException e) {
			// OK
		}
		return this;
	}

	@SuppressWarnings("rawtypes")
	private JsonObjectAssert pathIsEmpty(String path, String msg) {
		try {
			Object value = JsonPath.read(actual.toString(), path);
			if (value instanceof Map && ((Map) value).size() > 0) {
				fail(msg + " The value at path {" + path + "} is not empty, while should be.");
			} else if (value instanceof Collection && ((Collection) value).size() > 0) {
				fail(msg + " The value at path {" + path + "} is not empty, while should be.");
			}			
		} catch (PathNotFoundException e) {
			fail(msg + " The value at path {" + path + "} was not present but it should be present but empty.");
		}
		return this;
	}

	/**
	 * Assert that the string value which is present at the given path matches the pattern of a uuid.
	 *
	 * @param path
	 * @return Fluent API
	 */
	public JsonObjectAssert pathIsUuid(String path) {
		return pathIsUuid(path, null);
	}

	public JsonObjectAssert pathIsDate(String path, String msg) {
		if (msg == null) {
			msg = "";
		}
		String value = JsonPath.read(actual.toString(), path);
		assertNotNull("Value on path {" + path + "} was null", value);
		assertTrue("The specified value {" + value + "} on path {" + path + "} was no date: " + msg, DateUtils.isDate(value));
		return this;
	}

	public JsonObjectAssert pathIsUuid(String path, String msg) {
		if (msg == null) {
			msg = "";
		}
		String value = JsonPath.read(actual.toString(), path);
		assertNotNull("Value on path {" + path + "} was null", value);
		assertTrue("The specified value {" + value + "} on path {" + path + "} was no uuid: " + msg, UUIDUtil.isUUID(value));
		return this;
	}

	public JsonObjectAssert pathIsNotNull(String path) {
		return pathIsNotNull(path, null);
	}

	public JsonObjectAssert pathIsNotNull(String path, String msg) {
		if (msg == null) {
			msg = "";
		}
		Object value = JsonPath.read(actual.toString(), path);
		assertNotNull("Value on the path {" + path + "} was expected to be non-null: " + msg, value);
		return this;
	}

	public JsonObjectAssert pathIsNull(String path) {
		return pathIsNull(path, null);
	}

	public JsonObjectAssert pathIsNull(String path, String msg) {
		if (msg == null) {
			msg = "";
		}
		Object value = JsonPath.read(actual.toString(), path);
		assertNull("Value on the path {" + path + "} was expected to be null but was {" + value + "}: " + msg, value);
		return this;
	}

	public JsonObjectAssert hasPermFailure(String path) {
		JsonArray errors = actual.getJsonArray("errors");
		for (int i = 0; i < errors.size(); i++) {
			JsonObject error = errors.getJsonObject(i);
			if (path.equalsIgnoreCase(error.getString("path"))) {
				assertTrue("The error for path {" + path + "} did not contain location information.", error.containsKey("locations"));

				assertEquals("The message of the found error \n{" + error.encodePrettily() + "}", "graphql_error_missing_perm",
					error.getString("message"));
				assertEquals("The type of the found error \n{" + error.encodePrettily() + "} did not match.", "missing_perm",
					error.getString("type"));
				// assertEquals(uuid, error.getString("elementId"))
				assertEquals("The type within the found error \n{" + error.encodePrettily() + "} did not match.", "node",
					error.getString("elementType"));
				return this;
			}
		}
		fail("Perm error for path {" + path + "} could not be found.");
		return this;
	}

	public JsonObjectAssert hasNoGraphQLSyntaxError() {
		JsonArray errors = actual.getJsonArray("errors");
		if (errors == null) {
			return this;
		}
		for (int i = 0; i < errors.size(); i++) {
			JsonObject error = errors.getJsonObject(i);
			if ("InvalidSyntax".equalsIgnoreCase(error.getString("type"))) {
				fail("Found syntax error {\n" + error.encodePrettily() + "}\n");
			}
		}
		return this;
	}
}
