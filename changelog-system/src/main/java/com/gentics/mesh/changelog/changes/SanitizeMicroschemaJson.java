package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.OUT;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.json.JsonObject;

/**
 * Changelog entry which sanitizes the stores JSON schema.
 */
public class SanitizeMicroschemaJson extends AbstractChange {

	@Override
	public String getName() {
		return "Sanitize stored microschema JSON";
	}

	@Override
	public String getDescription() {
		return "Remove bogus fields from stored microschema JSON";
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
				schema.remove("editor");
				schema.remove("edited");
				schema.remove("creator");
				schema.remove("created");
				schema.remove("rolePerms");
				schema.remove("permissions");
				microschemaVersion.setProperty("json", schema.toString());
			}
		}

	}

	@Override
	public String getUuid() {
		return "7F1BE0A16BE042719BE0A16BE02271C1";
	}

}
