package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.OUT;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Vertex;

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
	public void applyInTx() {
		Vertex meshRoot = getMeshRootVertex();
		updateSchemas(meshRoot);
		updateMicroschemas(meshRoot);
	}

	private void updateMicroschemas(Vertex meshRoot) {
		Vertex microschemaRoot = meshRoot.getVertices(OUT, "HAS_MICROSCHEMA_ROOT").iterator().next();
		Iterator<Vertex> microschemaIt = microschemaRoot.getVertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM").iterator();
		while (microschemaIt.hasNext()) {
			Vertex microschemaVertex = microschemaIt.next();
			Iterator<Vertex> versionIt = microschemaVertex.getVertices(OUT, "HAS_PARENT_CONTAINER").iterator();
			while (versionIt.hasNext()) {
				Vertex schemaVersion = versionIt.next();

				// Update the version within the vertex
				int vertexVersion = schemaVersion.getProperty("version");
				schemaVersion.removeProperty("version");
				schemaVersion.setProperty("version", String.valueOf(vertexVersion) + ".0");

				// Update the version within the json
				String json = schemaVersion.getProperty("json");
				JsonObject schema = new JsonObject(json);
				int version = schema.getInteger("version");
				schema.remove("version");
				schema.put("version", String.valueOf(version) + ".0");
				schemaVersion.setProperty("json", schema.toString());
			}
		}
	}

	private void updateSchemas(Vertex meshRoot) {
		Vertex schemaRoot = meshRoot.getVertices(OUT, "HAS_ROOT_SCHEMA").iterator().next();
		Iterator<Vertex> schemaIt = schemaRoot.getVertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM").iterator();
		while (schemaIt.hasNext()) {
			Vertex schemaVertex = schemaIt.next();
			Iterator<Vertex> versionIt = schemaVertex.getVertices(OUT, "HAS_PARENT_CONTAINER").iterator();
			while (versionIt.hasNext()) {
				Vertex schemaVersion = versionIt.next();

				// Update the version within the vertex
				int vertexVersion = schemaVersion.getProperty("version");
				schemaVersion.removeProperty("version");
				schemaVersion.setProperty("version", String.valueOf(vertexVersion) + ".0");

				// Update the version within the json
				String json = schemaVersion.getProperty("json");
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
				schemaVersion.setProperty("json", schema.toString());
			}
		}
	}

	@Override
	public boolean requiresReindex() {
		return true;
	}
}
