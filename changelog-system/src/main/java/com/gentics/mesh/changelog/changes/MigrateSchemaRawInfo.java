package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;

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
		Iterator<Vertex> iter = meshRoot.vertices(OUT, "HAS_MICROSCHEMA_ROOT");
		if (!iter.hasNext()) {
			log.info("MigrateSchemaRawInfo change skipped");
			return;
		}
		Vertex microschemaRoot = iter.next();
		Iterator<Vertex> microschemaIt = microschemaRoot.vertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM");
		while (microschemaIt.hasNext()) {
			Vertex microschemaVertex = microschemaIt.next();
			Iterator<Vertex> versionIt = microschemaVertex.vertices(OUT, "HAS_PARENT_CONTAINER");
			while (versionIt.hasNext()) {
				Vertex microschemaVersion = versionIt.next();
				String json = microschemaVersion.<String>property("json").orElse(null);
				JsonObject schema = new JsonObject(json);
				migrateFields(schema);
				microschemaVersion.property("json", schema.toString());
			}
		}

		Vertex schemaRoot = meshRoot.vertices(OUT, "HAS_ROOT_SCHEMA").next();
		Iterator<Vertex> schemaIt = schemaRoot.vertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM");
		while (schemaIt.hasNext()) {
			Vertex schemaVertex = schemaIt.next();
			Iterator<Vertex> versionIt = schemaVertex.vertices(OUT, "HAS_PARENT_CONTAINER");
			while (versionIt.hasNext()) {
				Vertex schemaVersion = versionIt.next();
				String json = schemaVersion.<String>property("json").orElse(null);
				JsonObject schema = new JsonObject(json);
				migrateFields(schema);
				schemaVersion.property("json", schema.toString());
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
