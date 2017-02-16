 package com.gentics.mesh.search.index.role;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.trigramStringType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;

/**
 * Transformator for role search index documents.
 */
@Singleton
public class RoleTransformator extends AbstractTransformator<Role> {

	@Inject
	public RoleTransformator() {
	}

	/**
	 * Transform the given object into a source JSON object which can be used to store the document in the search provider specific format.
	 * 
	 * @param role
	 * @return JSON document representing the role
	 */
	@Override
	public JsonObject toDocument(Role role) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, role.getName());
		addBasicReferences(document, role);
		return document;
	}

	/**
	 * Return the type specific mapping properties.
	 */
	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramStringType());
		return props;
	}

}
