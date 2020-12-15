package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.OUT;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.json.JsonObject;

/**
 * Cleanup of the stored schema JSON. 
 */
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
	public void applyInTx() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex schemaRoot = meshRoot.getVertices(OUT, "HAS_ROOT_SCHEMA").iterator().next();
		Iterator<Vertex> schemaIt = schemaRoot.getVertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM").iterator();
		while (schemaIt.hasNext()) {
			Vertex schemaVertex = schemaIt.next();
			Iterator<Vertex> versionIt = schemaVertex.getVertices(OUT, "HAS_PARENT_CONTAINER").iterator();
			while (versionIt.hasNext()) {
				Vertex schemaVersion = versionIt.next();

				String json = schemaVersion.getProperty("json");
				JsonObject schema = new JsonObject(json);
				schema.remove("editor");
				schema.remove("edited");
				schema.remove("creator");
				schema.remove("created");
				schema.remove("rolePerms");
				schema.remove("permissions");
				schemaVersion.setProperty("json", schema.toString());
			}
		}

	}

	@Override
	public String getUuid() {
		return "34e86b5079e945ed991e799c0657181b";
	}

}
