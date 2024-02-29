package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.changelog.AbstractChange;

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
		Iterator<Vertex> iter = meshRoot.vertices(OUT, "HAS_ROOT_SCHEMA");
		if (!iter.hasNext()) {
			log.info("SanitizeSchemaJson change skipped");
			return;
		}
		Vertex schemaRoot = iter.next();
		Iterator<Vertex> schemaIt = schemaRoot.vertices(OUT, "HAS_SCHEMA_CONTAINER_ITEM");
		while (schemaIt.hasNext()) {
			Vertex schemaVertex = schemaIt.next();
			Iterator<Vertex> versionIt = schemaVertex.vertices(OUT, "HAS_PARENT_CONTAINER");
			while (versionIt.hasNext()) {
				Vertex schemaVersion = versionIt.next();

				String json = schemaVersion.<String>property("json").orElse(null);
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
