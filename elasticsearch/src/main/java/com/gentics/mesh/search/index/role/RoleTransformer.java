package com.gentics.mesh.search.index.role;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for role search index documents.
 */
@Singleton
public class RoleTransformer extends AbstractTransformer<Role> {

	@Inject
	public RoleTransformer() {
	}

	public String generateVersion(Role role) {
		// No need to add users since the creator/editor edge affects the role version
		return ETag.hash(role.getElementVersion());
	}

	/**
	 * Transform the role to the document which can be stored in ES.
	 * 
	 * @param role
	 * @param withVersion
	 *            Whether to include the version number.
	 * @return
	 */
	private JsonObject toDocument(Role role, boolean withVersion) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, role.getName());
		addBasicReferences(document, role);
		addPermissionInfo(document, role);
		if (withVersion) {
			document.put(VERSION_KEY, generateVersion(role));
		}
		return document;
	}

	/**
	 * Transform the given object into a source JSON object which can be used to store the document in the search provider specific format.
	 * 
	 * @param role
	 * @return JSON document representing the role
	 */
	@Override
	public JsonObject toDocument(Role role) {
		return toDocument(role, true);
	}

}
