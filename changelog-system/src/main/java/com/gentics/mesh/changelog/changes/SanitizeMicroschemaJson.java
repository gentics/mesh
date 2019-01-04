package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import io.vertx.core.json.JsonObject;

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
	public void apply() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex microschemaRoot = meshRoot.vertices(OUT, "HAS_MICROSCHEMA_ROOT").next();
		Iterator<Vertex> microschemaIt = microschemaRoot.vertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM");
		while (microschemaIt.hasNext()) {
			Vertex microschemaVertex = microschemaIt.next();
			Iterator<Vertex> versionIt = microschemaVertex.vertices(OUT, "HAS_PARENT_CONTAINER");
			while (versionIt.hasNext()) {
				Vertex microschemaVersion = versionIt.next();

				String json = microschemaVersion.value("json");
				JsonObject schema = new JsonObject(json);
				schema.remove("editor");
				schema.remove("edited");
				schema.remove("creator");
				schema.remove("created");
				schema.remove("rolePerms");
				schema.remove("permissions");
				microschemaVersion.property("json", schema.toString());
			}
		}

	}

	@Override
	public String getUuid() {
		return "7F1BE0A16BE042719BE0A16BE02271C1";
	}

}
