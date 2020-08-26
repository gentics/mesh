package com.gentics.mesh.search.index.role;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for role search index documents.
 */
@Singleton
public class RoleTransformer extends AbstractTransformer<HibRole> {

	@Inject
	public RoleTransformer() {
	}

	public String generateVersion(HibRole role) {
		// No need to add users since the creator/editor edge affects the role version
		return ETag.hash(role.getElementVersion());
	}

	/**
	 * Transform the given object into a source JSON object which can be used to store the document in the search provider specific format.
	 * 
	 * @param role
	 * @return JSON document representing the role
	 */
	@Override
	public JsonObject toDocument(HibRole role) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, role.getName());
		addBasicReferences(document, role);
		addPermissionInfo(document, role);
		document.put(VERSION_KEY, generateVersion(role));
		return document;
	}

}
