package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;

import io.vertx.core.json.JsonObject;

public class ChangeSchemaVersionType extends AbstractChange {
	@Override
	public String getUuid() {
		return "904F55D71CC54B388F55D71CC5BB38D1";
	}

	@Override
	public String getName() {
		return "Change the schema version type";
	}

	@Override
	public String getDescription() {
		return "Changes the schema version type from int to string";
	}

	@Override
	public void apply() {
		Vertex meshRoot = getMeshRootVertex();
		updateSchemas(meshRoot);
		updateMicroschemas(meshRoot);
	}

	private void updateMicroschemas(Vertex meshRoot) {
		Vertex microschemaRoot = meshRoot.vertices(OUT, "HAS_MICROSCHEMA_ROOT").next();
		Iterator<Vertex> microschemaIt = microschemaRoot.vertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM");
		while (microschemaIt.hasNext()) {
			Vertex microschemaVertex = microschemaIt.next();
			Iterator<Vertex> versionIt = microschemaVertex.vertices(OUT, "HAS_PARENT_CONTAINER");
			while (versionIt.hasNext()) {
				Vertex schemaVersion = versionIt.next();

				// Update the version within the vertex
				int vertexVersion = schemaVersion.value("version");
				schemaVersion.property("version").remove();
				schemaVersion.property("version", String.valueOf(vertexVersion) + ".0");

				// Update the version within the json
				String json = schemaVersion.value("json");
				JsonObject schema = new JsonObject(json);
				int version = schema.getInteger("version");
				schema.remove("version");
				schema.put("version", String.valueOf(version) + ".0");
				schemaVersion.property("json", schema.toString());
			}
		}
	}

	private void updateSchemas(Vertex meshRoot) {
		Vertex schemaRoot = meshRoot.vertices(OUT, "HAS_ROOT_SCHEMA").next();
		Iterator<Vertex> schemaIt = schemaRoot.vertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM");
		while (schemaIt.hasNext()) {
			Vertex schemaVertex = schemaIt.next();
			Iterator<Vertex> versionIt = schemaVertex.vertices(OUT, "HAS_PARENT_CONTAINER");
			while (versionIt.hasNext()) {
				Vertex schemaVersion = versionIt.next();

				// Update the version within the vertex
				int vertexVersion = schemaVersion.value("version");
				schemaVersion.property("version").remove();
				schemaVersion.property("version", String.valueOf(vertexVersion) + ".0");

				// Update the version within the json
				String json = schemaVersion.value("json");
				JsonObject schema = new JsonObject(json);

				Object versionValue = schema.getValue("version");
				schema.remove("version");
				if (versionValue instanceof String) {
					int version = Integer.valueOf((String) versionValue);
					schema.put("version", String.valueOf(version) + ".0");
				} else {
					int version = Integer.valueOf((Integer) versionValue);
					schema.put("version", String.valueOf(version) + ".0");
				}
				schemaVersion.property("json", schema.toString());
			}
		}
	}

	@Override
	public boolean requiresReindex() {
		return true;
	}
}
