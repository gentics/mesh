package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import io.vertx.core.json.JsonObject;

public class ChangeSanitizeSchemaJson extends AbstractChange {

	@Override
	public String getName() {
		return "Sanitize stored schema JSON";
	}

	@Override
	public String getDescription() {
		return "Remove bogus fields from stored schema JSON";
	}

	@Override
	public void apply() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex schemaRoot = meshRoot.vertices(OUT, "HAS_ROOT_SCHEMA").next();
		Iterator<Vertex> schemaIt = schemaRoot.vertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM");
		while (schemaIt.hasNext()) {
			Vertex schemaVertex = schemaIt.next();
			Iterator<Vertex> versionIt = schemaVertex.vertices(OUT, "HAS_PARENT_CONTAINER");
			while (versionIt.hasNext()) {
				Vertex schemaVersion = versionIt.next();

				String json = schemaVersion.value("json");
				JsonObject schema = new JsonObject(json);
				schema.remove("editor");
				schema.remove("edited");
				schema.remove("creator");
				schema.remove("created");
				schema.remove("rolePerms");
				schema.remove("permissions");
				schemaVersion.property("json", schema.toString());
			}
		}

	}

	@Override
	public String getUuid() {
		return "34e86b5079e945ed991e799c0657181b";
	}

}
