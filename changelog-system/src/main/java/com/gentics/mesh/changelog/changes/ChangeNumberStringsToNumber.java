package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class ChangeNumberStringsToNumber extends AbstractChange {
	private static final String NUMBER_TYPE = "number";
	private static final String LIST_TYPE = "list";
	private static final String SCHEMA_FIELDS = "fields";
	private static final String FIELD_TYPE_KEY = "type";
	private static final String FIELD_LIST_TYPE_KEY = "listType";
	private static final String FIELD_NAME_KEY = "name";

	private static final String UUID = "uuid";
	private static final String FIELD_KEY = "fieldkey";
	private static final String JSON_FIELD = "json";
	private static final String SCHEMA_CONTAINER_VERSION_CLASS = "SchemaContainerVersionImpl";
	private static final String HAS_SCHEMA_CONTAINER_VERSION = "HAS_SCHEMA_CONTAINER_VERSION";
	private static final String MICROSCHEMA_CONTAINER_VERSION_CLASS = "MicroschemaContainerVersionImpl";
	private static final String HAS_MICROSCHEMA_CONTAINER = "HAS_MICROSCHEMA_CONTAINER";
	private static final String HAS_LIST = "HAS_LIST";
	private static final String ITEM_PREFIX = "item-";

	private static final Logger log = LoggerFactory.getLogger(ChangeNumberStringsToNumber.class);

	private static final NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);

	private final Map<String, Schema> schemaMap = new HashMap<>();

	@Override
	public String getName() {
		return "Change Number String to Number";
	}

	@Override
	public String getDescription() {
		return "Changes the values of number fields (and number list fields) from strings to actual numbers.";
	}


	private Schema buildSchemaFromVertex(Vertex schemaVertex, String className) {
		return schemaMap.computeIfAbsent(schemaVertex.getProperty(UUID), uuid -> {
			Schema schema = new Schema();
			schema.type = className;
			schema.uuid = uuid;
			Object val = schemaVertex.getProperty("version");
			schema.version = String.valueOf(val);

			String json = schemaVertex.getProperty(JSON_FIELD);
			if (json == null) {
				return schema;
			}
			JsonObject jsonSchema = new JsonObject(json);
			if (!jsonSchema.containsKey(SCHEMA_FIELDS)) {
				return schema;
			}
			schema.name = jsonSchema.getString("name");
			JsonArray fields = jsonSchema.getJsonArray(SCHEMA_FIELDS);
			schema.fieldMap = IntStream.range(0, fields.size())
					.mapToObj(fields::getJsonObject)
					.filter(f -> {
						String type = f.getString(FIELD_TYPE_KEY);
						return NUMBER_TYPE.equals(type) || (LIST_TYPE.equals(type) && NUMBER_TYPE.equals(f.getString(FIELD_LIST_TYPE_KEY)));
					})
					.collect(Collectors.toMap(o -> o.getString(FIELD_NAME_KEY), Function.identity()));
			return schema;
		});
	}

	private void updateProperty(String propertyName, Vertex node) {
		Object obj = node.getProperty(propertyName);
		if (obj == null) {
			return;
		}
		if (!(obj instanceof String)) {
			if (log.isDebugEnabled()) {
				log.debug("Property '{}' for node '{}' in database is no string so we don't convert it. {}: '{}'", propertyName, node.getProperty(UUID), obj.getClass(), obj);
			}
			return;
		}
		String strVal = (String) obj;
		Number numVal;
		try {
			numVal = format.parse(strVal);
		} catch (ParseException e) {
			log.warn("Could not parse the number '{}', for field '{}' in node {}", strVal, propertyName, node.getId());
			numVal = 0;
		}
		node.removeProperty(propertyName);
		node.setProperty(propertyName, numVal);
	}

	private void updateLists(Vertex container, Map<String, JsonObject> fieldMap) {
		for (Vertex listElement: container.getVertices(Direction.OUT, HAS_LIST)) {
			String fieldName = listElement.getProperty(FIELD_KEY);
			if (fieldMap.containsKey(fieldName) && NUMBER_TYPE.equals(fieldMap.get(fieldName).getString(FIELD_LIST_TYPE_KEY))) {
				listElement.getPropertyKeys().stream()
						.filter(k -> k.startsWith(ITEM_PREFIX))
						.forEach(k -> updateProperty(k, listElement));
			}
		}
	}

	private void updateFields(Vertex container, Map<String, JsonObject> fieldMap) {
		fieldMap.entrySet().stream()
				.map(Map.Entry::getValue)
				.filter(f -> NUMBER_TYPE.equals(f.getString(FIELD_TYPE_KEY)))
				.forEach(f -> updateProperty(f.getString(FIELD_NAME_KEY) + "-" + NUMBER_TYPE, container));
	}


	public void updateVerticesForSchema(Vertex schemaVertex, Map<String, JsonObject> fieldMap, String label) {
		long count = 0;
		for (Vertex vertex : schemaVertex.getVertices(Direction.IN, label)) {
			count++;
			updateFields(vertex, fieldMap);
			updateLists(vertex, fieldMap);
			if (count % 10000 == 0) {
				log.debug("Commit the changes for the last 10.000 vertices to database...");
				getGraph().commit();
				log.info("Updated vertices {}", count);
			}
		}
	}

	public void convertViaSchema(String schemaVersionClassName, String label) {
		for (Vertex schemaVertex : getGraph().getVertices("@class", schemaVersionClassName)) {
			Schema schema = buildSchemaFromVertex(schemaVertex, schemaVersionClassName);
			if (!schema.fieldMap.isEmpty()) {
				log.info("Update vertices for {}", schema);
				updateVerticesForSchema(schemaVertex, schema.fieldMap, label);
				log.debug("Commit the changes for the remaining vertices of schema {} to database...", schema);
				getGraph().commit();
			}
		}
	}

	@Override
	public void applyInTx() {
		log.info("Start converting numbers in nodes.");
		convertViaSchema(SCHEMA_CONTAINER_VERSION_CLASS, HAS_SCHEMA_CONTAINER_VERSION);
		log.info("Start converting numbers in micro-nodes.");
		convertViaSchema(MICROSCHEMA_CONTAINER_VERSION_CLASS, HAS_MICROSCHEMA_CONTAINER);
	}

	@Override
	public String getUuid() {
		return "3F367427D10641FAB67427D10621FA90";
	}

	private class Schema {
		String type;
		String name;
		String uuid;
		String version;
		Map<String, JsonObject> fieldMap;

		@Override
		public String toString() {
			return type + "{" +
					"name='" + name + '\'' +
					", uuid='" + uuid + '\'' +
					", version='" + version + '\'' +
					'}';
		}
	}
}
