package com.gentics.mesh.plugin.common.model;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test automatic creation of {@link FieldMap FieldMaps} and
 * {@link NodeCreateRequest NodeCreateRequests} via the {@link RequestField}
 * annotations.
 */
public class CreateRequestModelTest {

	private static class TestRequest extends CreateRequestModel {

		@RequestField
		List<String> strings = Arrays.asList("Foo", "Bar");

		@RequestField
		List<Boolean> booleans = Arrays.asList(false, true);

		@RequestField
		List<Integer> integers = Arrays.asList(1, 2, 3);

		@RequestField
		List<Double> doubles = Arrays.asList(1.1, 2.2, 3.3);

		@RequestField
		List<JsonObject> jsonObjects = Arrays.asList(
			new JsonObject().put("one", "a").put("two", "b"),
			new JsonObject().put("three", 3).put("four", 4));

		@RequestField(date = true)
		List<String> dates = Arrays.asList(
			DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)),
			DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.ofInstant(Instant.now().plus(1, ChronoUnit.MINUTES), ZoneOffset.UTC)));

		@RequestField(html = true)
		List<String> htmlList = Arrays.asList(
			"<p>Hello there!</p>",
			"<p>General Kenobi!</p>");

		@RequestField
		JsonObject jsonObject = new JsonObject().put("field", "value");

		@RequestField
		JsonArray jsonArray = new JsonArray().add("One").add(new JsonObject().put("two", 2));

		@RequestField
		int integer = 23;

		@RequestField
		double aDouble = .5;

		@RequestField
		boolean aBoolean = true;

		@RequestField
		Boolean emptyBool;

		@RequestField
		String string = "Foo";

		@RequestField
		String nullString;

		@RequestField
		String emptyString = "";

		@RequestField(date = true)
		String date = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime
			.of(1989, 12, 13, 0, 0)
			.atZone(ZoneId.of("UTC")));

		@RequestField(html = true)
		String html = "<h1>Hello there!</h1>";

		@Override
		public String getSchemaName() {
			return "test_request_schema";
		}
	}

	@Test
	public void testDefaultCreateRequest() {
		TestRequest testRequest = new TestRequest();
		NodeCreateRequest nodeCreateRequest = testRequest.toNodeCreateRequest();

		assertThat(nodeCreateRequest.getLanguage())
			.as("Request language")
			.isEqualTo("en");

		assertThat(nodeCreateRequest.getSchema().getName())
			.as("Request schema")
			.isEqualTo(testRequest.getSchemaName());

		assertThat(nodeCreateRequest.getParentNode())
			.as("Parent UUID")
			.isNull();
	}

	@Test
	public void testCreateRequestWithParent() {
		String parentUuid = "fake-uuid";
		TestRequest testRequest = new TestRequest();
		NodeCreateRequest nodeCreateRequest = testRequest.toNodeCreateRequest(parentUuid);

		assertThat(nodeCreateRequest.getParentNode().getUuid())
			.as("Parent UUID")
			.isEqualTo(parentUuid);
	}

	@Test
	public void testFieldMap() {
		TestRequest request = new TestRequest();
		FieldMap fields = request.toFieldMap();

		assertThat(fields.getStringFieldList("strings").getItems())
			.as("String list")
			.containsExactlyElementsOf(request.strings);

		assertThat(fields.getBooleanFieldList("booleans").getItems())
			.as("Boolean list")
			.containsExactlyElementsOf(request.booleans);

		assertThat(fields.getNumberFieldList("integers").getItems())
			.as("Integer list")
			.containsExactlyElementsOf(request.integers);

		assertThat(fields.getNumberFieldList("doubles").getItems())
			.as("Double list")
			.containsExactlyElementsOf(request.doubles);

		assertThat(fields.getStringFieldList("jsonObjects").getItems())
			.as("JsonObject list")
			.containsExactlyElementsOf(request.jsonObjects.stream().map(JsonObject::encode).collect(Collectors.toList()));

		assertThat(fields.getDateFieldList("dates").getItems())
			.as("Date list")
			.containsExactlyElementsOf(request.dates);

		assertThat(fields.getHtmlFieldList("htmlList").getItems())
			.as("HTML list")
			.containsExactlyElementsOf(request.htmlList);

		assertThat(fields.getStringField("jsonObject").getString())
			.as("JsonObject")
			.isEqualTo(request.jsonObject.encode());

		assertThat(fields.getStringField("jsonArray").getString())
			.as("JsonArray")
			.isEqualTo(request.jsonArray.encode());

		assertThat(fields.getNumberField("integer").getNumber())
			.as("Integer")
			.isEqualTo(request.integer);

		assertThat(fields.getNumberField("aDouble").getNumber())
			.as("Double")
			.isEqualTo(request.aDouble);

		assertThat(fields.getBooleanField("aBoolean").getValue())
			.as("Boolean")
			.isEqualTo(request.aBoolean);

		assertThat(fields.getStringField("string").getString())
			.as("String")
			.isEqualTo(request.string);

		assertThat(fields.getStringField("emptyString").getString())
			.as("Empty string")
			.isEqualTo(request.emptyString);

		assertThat(fields.getDateField("date").getDate())
			.as("Date")
			.isEqualTo(request.date);

		assertThat(fields.getHtmlField("html").getHTML())
			.as("HTML")
			.isEqualTo(request.html);

		assertThat(fields.hasField("emptyBoolean"))
			.as("Empty boolean")
			.isFalse();

		assertThat(fields.hasField("nullString"))
			.as("Null string")
			.isFalse();
	}
}
