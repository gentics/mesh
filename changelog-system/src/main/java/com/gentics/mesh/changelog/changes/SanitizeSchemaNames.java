package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.OUT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.json.JsonObject;

public class SanitizeSchemaNames extends AbstractChange {

	@Override
	public String getName() {
		return "Sanitize stored schema and microschema name";
	}

	@Override
	public String getDescription() {
		return "Replaces no longer allowed characters within the schema and microschema name";
	}

	@Override
	public void actualApply() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex microschemaRoot = meshRoot.getVertices(OUT, "HAS_MICROSCHEMA_ROOT").iterator().next();
		Iterator<Vertex> microschemaIt = microschemaRoot.getVertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM").iterator();
		while (microschemaIt.hasNext()) {
			Vertex microschemaVertex = microschemaIt.next();
			fixName(microschemaVertex);
			Iterator<Vertex> versionIt = microschemaVertex.getVertices(OUT, "HAS_PARENT_CONTAINER").iterator();
			while (versionIt.hasNext()) {
				Vertex microschemaVersion = versionIt.next();
				fixName(microschemaVersion);
				String json = microschemaVersion.getProperty("json");
				JsonObject schema = new JsonObject(json);
				String name = schema.getString("name");
				name = name.replaceAll("-", "_");
				schema.put("name", name);
				microschemaVersion.setProperty("json", schema.toString());
			}
		}

		Vertex schemaRoot = meshRoot.getVertices(OUT, "HAS_ROOT_SCHEMA").iterator().next();
		Iterator<Vertex> schemaIt = schemaRoot.getVertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM").iterator();
		while (schemaIt.hasNext()) {
			Vertex schemaVertex = schemaIt.next();
			fixName(schemaVertex);
			Iterator<Vertex> versionIt = schemaVertex.getVertices(OUT, "HAS_PARENT_CONTAINER").iterator();
			while (versionIt.hasNext()) {
				Vertex schemaVersion = versionIt.next();
				fixName(schemaVersion);
				String json = schemaVersion.getProperty("json");
				JsonObject schema = new JsonObject(json);
				String name = schema.getString("name");
				name = name.replaceAll("-", "_");
				schema.put("name", name);
				schemaVersion.setProperty("json", schema.toString());
			}
		}

	}

	private void fixName(Vertex schemaVertex) {
		String name = schemaVertex.getProperty("name");
		if (!isEmpty(name)) {
			name = name.replaceAll("-", "_");
			schemaVertex.setProperty("name", name);
		}
	}

	@Override
	public String getUuid() {
		return "52367EB3E028450BB67EB3E028550B39";
	}

}