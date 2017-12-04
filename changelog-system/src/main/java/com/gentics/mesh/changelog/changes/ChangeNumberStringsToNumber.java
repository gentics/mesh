package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;
import com.syncleus.ferma.traversals.SimpleTraversal;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Locale;

/**
 * Created by sebastian on 04.12.17.
 */
public class ChangeNumberStringsToNumber extends AbstractChange {

	private static final Logger log = LoggerFactory.getLogger(ChangeNumberStringsToNumber.class);

	private static final NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);

	@Override
	public String getName() {
		return "Change Number String to Number";
	}

	@Override
	public String getDescription() {
		return "Changes the values of number fields (and number list fields) from strings to actual numbers.";
	}

	private JsonObject getSchemaContainerVersion(Vertex fieldContainer, String label) {
		Iterator<Vertex> schemaContainerVersionIt = fieldContainer.getVertices(Direction.OUT, label).iterator();
		if (schemaContainerVersionIt.hasNext()) {
			return new JsonObject(schemaContainerVersionIt.next().getProperty("json").toString());
		}
		return null;
	}


	private void updateField(Vertex node, JsonObject field) {
		String fieldName = field.getString("name");
		if ("number".equals(field.getString("type")) && node.getProperty(fieldName) != null) {
			String strVal = node.getProperty(fieldName);
			Number numVal;
			try {
				numVal = format.parse(strVal);
			} catch (ParseException e) {
				log.warn("Could not parse the number '{}', for field '{}' in field-container {}", strVal, fieldName, node.getId());
				numVal = 0;
			}
			node.removeProperty(fieldName);
			node.setProperty(fieldName, numVal);
		} else if ("list".equals(field.getString("type")) && "number".equals(field.getString("listType"))) {
			for (Vertex list: node.getVertices(Direction.OUT, "HAS_LIST")) {

			}
		}
	}

	private void updateFields(Vertex node, JsonObject schema) {
		if (!schema.containsKey("fields")) {
			return;
		}
		JsonArray fields = schema.getJsonArray("fields");
		for (int i = 0; i < fields.size(); i++) {


			JsonObject field = fields.getJsonObject(i);
			updateField(node, field);
		}
	}


	private void updateNode(Vertex node, String type) {
		JsonObject schema = getSchemaContainerVersion(node,"node".equals(type) ? "HAS_SCHEMA_CONTAINER_VERSION" : "HAS_MICROSCHEMA_CONTAINER");
		if (schema == null) {
			log.info("No schema found for {} with uuid: {}", type, node.getId());
		} else {
			updateFields(node, schema);
		}
	}

	@Override
	public void apply() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex nodeRoot = meshRoot.getVertices(Direction.OUT, "HAS_NODE_ROOT").iterator().next();
		for (Vertex nodeImpl: nodeRoot.getVertices(Direction.OUT, "HAS_NODE")) {
			for (Vertex fieldContainer : nodeImpl.getVertices(Direction.OUT, "HAS_FIELD_CONTAINER")) {
				updateNode(fieldContainer, "node");
			}
		}
		for (Vertex micronode : getGraph().getVertices("ferma_type", "MicronodeImpl")) {
			updateNode(micronode, "micronode");
		}
	}

	@Override
	public String getUuid() {
		return "3F367427D10641FAB67427D10621FA90";
	}
}
