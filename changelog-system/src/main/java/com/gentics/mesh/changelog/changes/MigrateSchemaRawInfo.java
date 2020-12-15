package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.OUT;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Changelog entry for migrating the raw info from schemas. 
 */
public class MigrateSchemaRawInfo extends AbstractChange {

	@Override
	public String getUuid() {
		return "4417B2EBB2934A8597B2EBB2934A8515";
	}

	@Override
	public String getName() {
		return "migrate-raw-info";
	}

	@Override
	public String getDescription() {
		return "Removes the raw info from the schema and replaces it with the previously used elasticsearch field mapping definition";
	}

	@Override
	public void applyInTx() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex microschemaRoot = meshRoot.getVertices(OUT, "HAS_MICROSCHEMA_ROOT").iterator().next();
		Iterator<Vertex> microschemaIt = microschemaRoot.getVertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM").iterator();
		while (microschemaIt.hasNext()) {
			Vertex microschemaVertex = microschemaIt.next();
			Iterator<Vertex> versionIt = microschemaVertex.getVertices(OUT, "HAS_PARENT_CONTAINER").iterator();
			while (versionIt.hasNext()) {
				Vertex microschemaVersion = versionIt.next();
				String json = microschemaVersion.getProperty("json");
				JsonObject schema = new JsonObject(json);
				migrateFields(schema);
				microschemaVersion.setProperty("json", schema.toString());
			}
		}

		Vertex schemaRoot = meshRoot.getVertices(OUT, "HAS_ROOT_SCHEMA").iterator().next();
		Iterator<Vertex> schemaIt = schemaRoot.getVertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM").iterator();
		while (schemaIt.hasNext()) {
			Vertex schemaVertex = schemaIt.next();
			Iterator<Vertex> versionIt = schemaVertex.getVertices(OUT, "HAS_PARENT_CONTAINER").iterator();
			while (versionIt.hasNext()) {
				Vertex schemaVersion = versionIt.next();
				String json = schemaVersion.getProperty("json");
				JsonObject schema = new JsonObject(json);
				migrateFields(schema);
				schemaVersion.setProperty("json", schema.toString());
			}
		}
	}

	/**
	 * Remove the old search index field and add the new field which contains the the needed mapping.
	 * 
	 * @param schema
	 */
	private void migrateFields(JsonObject schema) {
		JsonArray fields = schema.getJsonArray("fields");
		for (int i = 0; i < fields.size(); i++) {
			JsonObject field = fields.getJsonObject(i);
			JsonObject options = field.getJsonObject("searchIndex");
			if (options != null) {
				Boolean flag = options.getBoolean("addRaw");
				field.remove("searchIndex");
				if (flag != null && flag == true) {
					field.put("elasticsearch", new JsonObject().put("raw", new JsonObject().put("index", "not_analyzed").put("type", "string")));
				}
			}
		}
	}

	@Override
	public boolean requiresReindex() {
		return true;
	}

}
